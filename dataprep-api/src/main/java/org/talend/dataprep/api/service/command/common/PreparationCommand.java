package org.talend.dataprep.api.service.command.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.api.preparation.Step;
import org.talend.dataprep.api.service.command.dataset.DataSetGet;
import org.talend.dataprep.preparation.store.ContentCache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.netflix.hystrix.HystrixCommandGroupKey;

public abstract class PreparationCommand<T> extends DataPrepCommand<T> {

    /**
     * <p>
     * A configuration to allow work from intermediate (cached) preparation content. If set to <b>true</b> and
     * preparation has:
     * <ul>
     *     <li>Root</li>
     *     <li>Step #1</li>
     *     <li>Step #2</li>
     * </ul>
     * When user asks for content @ Step #2, a more efficient approach is to start from content @ Step #1 (iso. starting
     * over from Root).
     * </p>
     * <p>
     * However, content cached for Step #1 can't be used as is (columns are located after "records", causing DataSet
     * deserialization to fail).
     * </p>
     */
    private static final boolean ALLOW_WORK_FROM_CACHE = false;

    @Autowired
    protected Jackson2ObjectMapperBuilder builder;

    @Autowired
    protected ContentCache contentCache;

    protected PreparationCommand(final HystrixCommandGroupKey groupKey, final HttpClient client) {
        super(groupKey, client);
    }

    /**
     * Call Preparation Service to get preparation details
     *
     * @param preparationId - the preparation id
     * @return the resulting Json node object
     * @throws java.io.IOException
     */
    protected Preparation getPreparation(final String preparationId) throws IOException {
        if (StringUtils.isEmpty(preparationId)) {
            throw new IllegalArgumentException("Preparation id cannot be empty.");
        }
        final HttpGet preparationRetrieval = new HttpGet(preparationServiceUrl + "/preparations/" + preparationId);
        try {
            InputStream content = client.execute(preparationRetrieval).getEntity().getContent();
            return builder.build().reader(Preparation.class).readValue(content);
        } finally {
            preparationRetrieval.releaseConnection();
        }
    }

    /**
     * Call Dataset Service to get dataset metadata details
     *
     * @param datasetId - the preparation id
     * @return the resulting Json node object
     * @throws java.io.IOException
     */
    protected JsonNode getDatasetDetails(final String datasetId) throws IOException {
        final HttpGet datasetRetrieval = new HttpGet(datasetServiceUrl + "/datasets/" + datasetId + "/metadata");
        try {
            InputStream content = client.execute(datasetRetrieval).getEntity().getContent();
            return getJsonReader().readTree(content);
        } finally {
            datasetRetrieval.releaseConnection();
        }
    }

    /**
     * Get dataset records
     *
     * @param dataSetId - the dataset id
     * @return the resulting input stream records
     */
    protected InputStream getDatasetContent(final String dataSetId) {
        final DataSetGet retrieveDataSet = context.getBean(DataSetGet.class, client, dataSetId, false, true);
        return retrieveDataSet.execute();
    }

    /**
     * Serialize the actions to string.
     *
     * @param stepActions - map of couple (stepId, action)
     * @return the serialized actions
     */
    protected String serializeActions(final Collection<Action> stepActions) throws JsonProcessingException {
        return "{\"actions\": " + getJsonWriter().writeValueAsString(stepActions) + "}";
    }

    /**
     * Serialize the list of integer to json string.
     * 
     * @param listToEncode - list of integer to encode
     * @return the serialized and encoded list
     */
    protected String serializeIds(final List<Integer> listToEncode) throws JsonProcessingException {
        return getJsonWriter().writeValueAsString(listToEncode);
    }

    /**
     * Encode the string to base 64
     *
     * @param toEncode The string to encode
     * @return the encoded string
     */
    protected String encode(String toEncode) {
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }

    protected List<Action> getPreparationActions(Preparation preparation, String stepId) throws IOException {
        if (StringUtils.isEmpty(stepId) || Step.ROOT_STEP.id().equals(stepId)) {
            // No need for a connection to retrieve an empty list.
            return Collections.emptyList();
        }
        final HttpGet actionsRetrieval = new HttpGet(preparationServiceUrl + "/preparations/" + preparation.id()
                + "/actions/" + stepId);
        try {
            InputStream content = client.execute(actionsRetrieval).getEntity().getContent();
            return builder
                    .build()
                    .reader(new TypeReference<List<Action>>() {
                    })
                    .readValue(content);
        } finally {
            actionsRetrieval.releaseConnection();
        }
    }

    public PreparationContext getContext(String preparationId, String stepId) throws IOException {
        PreparationContext ctx = new PreparationContext();
        if (preparationId == null) {
            ctx.actions = Collections.emptyList();
            ctx.version = Step.ROOT_STEP.id();
            return ctx;
        }
        // Modifies asked version with actual step id
        final Preparation preparation = getPreparation(preparationId);
        String version = stepId;
        if ("head".equals(stepId)) {
            int lastIndex = preparation.getSteps().size() - 1;
            version = preparation.getSteps().get(lastIndex);
        } else if ("origin".equals(version)) {
            version = Step.ROOT_STEP.id();
        }
        ctx.preparation = preparation;
        ctx.version = version;
        // Direct try on cache at given version
        if (contentCache.has(preparationId, version)) {
            ctx.content = contentCache.get(preparationId, version);
            ctx.actions = Collections.emptyList();
            ctx.fromCache = true;
            return ctx;
        }
        // At this point, content does *not* come from cache
        ctx.fromCache = false;
        String transformationStartStep;
        if (ALLOW_WORK_FROM_CACHE) {
            // Try to find intermediate cached version (starting from version)
            transformationStartStep = version;
            final List<String> preparationSteps = preparation.getSteps();
            for (String step : preparationSteps) {
                transformationStartStep = step;
                if (contentCache.has(preparationId, step)) {
                    ctx.content = contentCache.get(preparationId, step);
                    break;
                }
            }
            // Did not find any cache for retrieve preparation details, starts over from original dataset
            if (Step.ROOT_STEP.id().equals(transformationStartStep)) {
                final String dataSetId = preparation.getDataSetId();
                final DataSetGet retrieveDataSet = context.getBean(DataSetGet.class, client, dataSetId, false, true);
                ctx.content = retrieveDataSet.execute();
            }
        } else {
            // Don't allow to work from intermediate cached steps, so start over from root (data set content).
            final String dataSetId = preparation.getDataSetId();
            final DataSetGet retrieveDataSet = context.getBean(DataSetGet.class, client, dataSetId, false, true);
            ctx.content = retrieveDataSet.execute();
            transformationStartStep = stepId;
        }
        // Build the actions to execute
        if (Step.ROOT_STEP.id().equals(transformationStartStep)) {
            // Went down to root step and found nothing in cache -> get all preparation actions
            ctx.actions = getPreparationActions(preparation, stepId);
        } else {
            // Stopped in the middle -> compute list of actions to remove
            ctx.actions = getPreparationActions(preparation, transformationStartStep);
        }
        return ctx;
    }

    public class PreparationContext {

        boolean fromCache;

        InputStream content;

        List<Action> actions;

        Preparation preparation;

        String version;

        public InputStream getContent() {
            return content;
        }

        public List<Action> getActions() {
            return actions;
        }

        public Preparation getPreparation() {
            return preparation;
        }

        public String getVersion() {
            return version;
        }

        public boolean fromCache() {
            return fromCache;
        }
    }
}