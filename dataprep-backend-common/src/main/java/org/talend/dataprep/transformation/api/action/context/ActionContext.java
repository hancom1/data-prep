package org.talend.dataprep.transformation.api.action.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.api.action.metadata.common.ImplicitParameters;

/**
 * Context for an action within a transformation. Hence, several instance of the same action can have their own context.
 */
public class ActionContext {

    public enum ActionStatus {
        /**
         * Indicate action is good for usage and transformation process should continue using this action.
         */
        OK,
        /**
         * Indicates action is "canceled": transformation process should discard action from execution and won't execute
         * it.
         */
        CANCELED
    }

    /** Link to the transformation context. */
    private final TransformationContext parent;

    /** A map of object (used to reuse objects across row process). */
    private Map<String, Object> context = new HashMap<>();

    private RowMetadata outputRowMetadata;

    private RowMetadata inputRowMetadata;

    private Map<String, String> parameters = Collections.emptyMap();

    private ActionStatus actionStatus;

    public ActionContext(TransformationContext parent, RowMetadata rowMetadata) {
        this.parent = parent;
        this.inputRowMetadata = rowMetadata;
    }

    /**
     * Default constructor.
     *
     * @param parent the parent transformation context.
     */
    public ActionContext(TransformationContext parent) {
        this.parent = parent;
    }

    /**
     * @return the parent context.
     */
    public TransformationContext getParent() {
        return parent;
    }

    /**
     * <p>
     * Returns the column with logical name <code>name</code> or creates a new one with provided {@link Supplier
     * supplier}.
     * </p>
     * <p>
     * <b>Note</b>It is up to the caller to insert column in a {@link org.talend.dataprep.api.dataset.RowMetadata row}.
     * </p>
     *
     * @param name A column name as string. All values are accepted, no collision with other action can occur.
     * @param create A {@link Function function} that provides a new {@link ColumnMetadata} in case no column with
     * <code>name</code> was previously created. Function is <b>not</b> allowed to return <code>null</code>.
     * @return A {@link ColumnMetadata column} id with name <code>name</code>.
     * @throws IllegalArgumentException In case the <code>supplier</code> returned a <code>null</code> instance.
     */
    public String column(String name, Function<RowMetadata, ColumnMetadata> create) {
        String key = getColumnKey(name);
        if (context.containsKey(key)) {
            return ((ColumnMetadata) context.get(key)).getId();
        } else {
            final ColumnMetadata columnMetadata = create.apply(inputRowMetadata);
            if (columnMetadata == null) {
                throw new IllegalArgumentException("Cannot use a null column for '" + name + "'");
            }
            context.put(key, columnMetadata);
            return columnMetadata.getId();
        }
    }

    /**
     * Return the context key for the column.
     *
     * @param name the column original name.
     * @return the context key for the column.
     */
    private String getColumnKey(String name) {
        return "col#" + name;
    }

    /**
     * Return the object from the context or use the supplier to create it and cache it.
     *
     * @param key the object key.
     * @param parameters the parameters used by the supplier.
     * @param supplier the supplier to use to create the object in case it is not found in the context.
     * @return the object (stored in the context).
     */
    public <T> T get(String key, Map<String, String> parameters, Function<Map<String, String>, T> supplier) {
        if (context.containsKey(key)) {
            return (T) context.get(key);
        }

        final T value = supplier.apply(parameters);
        context.put(key, value);
        return value;
    }

    /**
     * @return the context entries.
     */
    public Collection<Object> getContextEntries() {
        return context.values();
    }

    /**
     * @return A new {@link ActionContext} instance that can not be modified: an immutable ActionContext will <b>not</b>
     * let action add a new column, but allows to get previously created ones.
     */
    public ActionContext asImmutable() {
        final ActionContext actionContext = new ActionContext(parent);
        actionContext.setInputRowMetadata(inputRowMetadata);
        actionContext.setOutputRowMetadata(outputRowMetadata);
        actionContext.context = Collections.unmodifiableMap(this.context);
        return actionContext;
    }

    public RowMetadata getInputRowMetadata() {
        return inputRowMetadata;
    }

    public void setOutputRowMetadata(RowMetadata outputRowMetadata) {
        this.outputRowMetadata = outputRowMetadata;
    }

    public RowMetadata getOutputRowMetadata() {
        return outputRowMetadata;
    }

    public void setInputRowMetadata(RowMetadata inputRowMetadata) {
        this.inputRowMetadata = inputRowMetadata;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getColumnId() {
        return parameters.get(ImplicitParameters.COLUMN_ID.getKey());
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Long getRowId() {
        return Long.parseLong(parameters.get(ImplicitParameters.ROW_ID.getKey()));
    }

    public DataSetRow getPreviousRow() {
        return parent.getPreviousRow();
    }

    public ActionStatus getActionStatus() {
        return actionStatus;
    }

    public void setActionStatus(ActionStatus actionStatus) {
        this.actionStatus = actionStatus;
    }
}
