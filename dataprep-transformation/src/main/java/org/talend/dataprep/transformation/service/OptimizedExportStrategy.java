package org.talend.dataprep.transformation.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.exception.TDPException;
import org.talend.dataprep.exception.error.TransformationErrorCodes;
import org.talend.dataprep.format.export.ExportFormat;
import org.talend.dataprep.transformation.api.transformer.configuration.Configuration;
import org.talend.dataprep.transformation.cache.TransformationCacheKey;
import org.talend.dataprep.transformation.cache.TransformationMetadataCacheKey;

import com.fasterxml.jackson.core.JsonParser;

/**
 * A {@link ExportStrategy strategy} to export a preparation (using its default data set), using any information
 * available in cache (metadata and content).
 */
@Component
public class OptimizedExportStrategy extends StandardExportStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimizedExportStrategy.class);

    @Override
    public int order() {
        return Integer.MIN_VALUE; // Ensure this is the first strategy tried.
    }

    @Override
    public boolean accept(ExportParameters parameters) {
        if (parameters == null) {
            return false;
        }
        if (parameters.getContent() != null) {
            return false;
        }
        final OptimizedPreparationInput optimizedPreparationInput = new OptimizedPreparationInput(parameters);
        return optimizedPreparationInput.applicable();
    }

    @Override
    public StreamingResponseBody execute(ExportParameters parameters) {
        final String formatName = parameters.getExportType();
        final ExportFormat format = getFormat(formatName);
        ExportUtils.setExportHeaders(parameters.getExportName(), format);

        return outputStream -> performOptimizedTransform(parameters, outputStream);
    }

    private void performOptimizedTransform(ExportParameters parameters, OutputStream outputStream) throws IOException {
        // Initial check
        OptimizedPreparationInput optimizedPreparationInput = new OptimizedPreparationInput(parameters).invoke();
        if (optimizedPreparationInput == null) {
            throw new IllegalStateException("Unable to use this strategy (call accept() before calling this).");
        }
        final String preparationId = parameters.getPreparationId();
        final String dataSetId = optimizedPreparationInput.getDataSetId();
        final TransformationCacheKey transformationCacheKey = optimizedPreparationInput.getTransformationCacheKey();
        final DataSetMetadata metadata = optimizedPreparationInput.getMetadata();
        final String previousVersion = optimizedPreparationInput.getPreviousVersion();
        final String version = optimizedPreparationInput.getVersion();
        final ExportFormat format = getFormat(parameters.getExportType());

        // Get content from previous step
        try (JsonParser parser = mapper.getFactory().createParser(contentCache.get(transformationCacheKey))) {
            // Create dataset
            final DataSet dataSet = mapper.readerFor(DataSet.class).readValue(parser);
            dataSet.setMetadata(metadata);

            // get the actions to apply (no preparation ==> dataset export ==> no actions)
            String actions = getActions(preparationId, previousVersion, version);

            LOGGER.debug("Running optimized strategy for preparation {} @ step #{}", preparationId, version);

            final TransformationCacheKey key = new TransformationCacheKey(preparationId, dataSetId, parameters.getExportType(),
                    version);
            LOGGER.debug("Cache key: " + key.getKey());
            LOGGER.debug("Cache key details: " + key.toString());

            TeeOutputStream tee = new TeeOutputStream(outputStream, contentCache.put(key, ContentCache.TimeToLive.DEFAULT));
            try {
                Configuration configuration = Configuration.builder() //
                        .args(parameters.getArguments()) //
                        .outFilter(rm -> filterService.build(parameters.getFilter(), rm)) //
                        .format(format.getName()) //
                        .actions(actions) //
                        .preparationId(preparationId) //
                        .stepId(version) //
                        .volume(Configuration.Volume.SMALL) //
                        .output(tee) //
                        .build();
                factory.get(configuration).transform(dataSet, configuration);
                tee.flush();
            } catch (Throwable e) { // NOSONAR
                contentCache.evict(key);
                throw e;
            }
        } catch (TDPException e) {
            throw e;
        } catch (Exception e) {
            throw new TDPException(TransformationErrorCodes.UNABLE_TO_TRANSFORM_DATASET, e);
        }
    }

    /**
     * A utility class to both extract information to run optimized strategy <b>and</b> check if there's enough information
     * to use the strategy.
     */
    private class OptimizedPreparationInput {

        private final String stepId;

        private final String preparationId;

        private final String dataSetId;

        private final String formatName;

        private final Preparation preparation;

        private String version;

        private DataSetMetadata metadata;

        private TransformationCacheKey transformationCacheKey;

        private String previousVersion;

        private OptimizedPreparationInput(ExportParameters parameters) {
            this.stepId = parameters.getStepId();
            this.preparationId = parameters.getPreparationId();
            if (preparationId != null) {
                this.preparation = getPreparation(preparationId);
            } else {
                preparation = null;
            }
            if (StringUtils.isEmpty(parameters.getDatasetId()) && preparation != null) {
                this.dataSetId = preparation.getDataSetId();
            } else {
                this.dataSetId = parameters.getDatasetId();
            }
            this.formatName = parameters.getExportType();
        }

        private String getDataSetId() {
            return dataSetId;
        }

        private String getVersion() {
            return version;
        }

        private DataSetMetadata getMetadata() {
            return metadata;
        }

        private TransformationCacheKey getTransformationCacheKey() {
            return transformationCacheKey;
        }

        private boolean applicable() {
            try {
                return invoke() != null;
            } catch (IOException e) {
                LOGGER.debug("Unable to check if optimized preparation path is applicable.", e);
                return false;
            }
        }

        private String getPreviousVersion() {
            return previousVersion;
        }

        // Extract information or returns null is not applicable.
        private OptimizedPreparationInput invoke() throws IOException {
            if (preparation == null) {
                // Not applicable (need preparation to work on).
                return null;
            }
            // head is not allowed as step id
            version = stepId;
            previousVersion = rootStep.getId();
            final List<String> steps = preparation.getSteps();
            if (steps.size() <= 2) {
                LOGGER.debug("Not enough steps ({}) in preparation.", steps.size());
                return null;
            }
            if (StringUtils.equals("head", stepId) || StringUtils.isEmpty(stepId)) {
                version = steps.get(steps.size() - 1);
                previousVersion = steps.get(steps.size() - 2);
            }
            // Get metadata of previous step
            TransformationMetadataCacheKey transformationMetadataCacheKey = new TransformationMetadataCacheKey(preparationId, previousVersion);
            if (contentCache.get(transformationMetadataCacheKey) == null) {
                LOGGER.debug("No metadata cached for previous version '{}' (key for lookup: '{}')", previousVersion,
                        transformationMetadataCacheKey.getKey());
                return null;
            }
            try (InputStream input = contentCache.get(transformationMetadataCacheKey)) {
                metadata = mapper.readerFor(DataSetMetadata.class).readValue(input);
            }
            transformationCacheKey = new TransformationCacheKey(preparationId, dataSetId, formatName, previousVersion);
            LOGGER.debug("Previous content cache key: " + transformationCacheKey.getKey());
            LOGGER.debug("Previous content cache key details: " + transformationCacheKey.toString());
            if (contentCache.get(transformationCacheKey) == null) {
                LOGGER.debug("No content cached for previous version '{}'", previousVersion);
                return null;
            }
            return this;
        }
    }

}
