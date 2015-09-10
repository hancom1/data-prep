package org.talend.dataprep.transformation.api.action.metadata.date;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.api.action.metadata.category.ActionCategory;
import org.talend.dataprep.transformation.api.action.metadata.common.AbstractActionMetadata;
import org.talend.dataprep.transformation.api.action.metadata.common.ActionMetadata;
import org.talend.dataprep.transformation.api.action.metadata.common.ColumnAction;
import org.talend.dataprep.transformation.api.action.parameters.Item;
import org.talend.dataprep.transformation.api.action.parameters.Parameter;

import javax.annotation.Nonnull;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.talend.dataprep.api.type.Type.STRING;

@Component(TimestampToDate.ACTION_BEAN_PREFIX + TimestampToDate.ACTION_NAME)
public class TimestampToDate extends AbstractActionMetadata implements ColumnAction, DatePatternParamModel {

    /**
     * The action name.
     */
    public static final String ACTION_NAME = "timestamp_to_date"; //$NON-NLS-1$

    /**
     * The column appendix.
     */
    public static final String APPENDIX = "_as_date"; //$NON-NLS-1$

    /**
     * @see ActionMetadata#getName()
     */
    @Override
    public String getName() {
        return ACTION_NAME;
    }

    /**
     * @see ActionMetadata#acceptColumn(ColumnMetadata)
     */
    @Override
    public boolean acceptColumn(ColumnMetadata column) {
        return Type.INTEGER.equals(Type.get(column.getType()));
    }

    /**
     * @see ActionMetadata#getCategory()
     */
    @Override
    public String getCategory() {
        return ActionCategory.DATE.getDisplayName();
    }

    @Nonnull
    @Override
    public Item[] getItems() {
        return getItemsForDatePattern();
    }

    /**
     * @see ColumnAction#applyOnColumn(DataSetRow, TransformationContext, Map, String)
     */
    @Override
    public void applyOnColumn(DataSetRow row, TransformationContext context, Map<String, String> parameters, String columnId) {

        String newPattern = getNewPattern(parameters);
        DateTimeFormatter newDateFormat = getDateFormat(newPattern);

        // create new column and append it after current column
        final RowMetadata rowMetadata = row.getRowMetadata();
        final ColumnMetadata column = rowMetadata.getById(columnId);
        final String newColumn = rowMetadata.insertAfter(columnId, createNewColumn(column));

        final String value = row.get(columnId);
        row.set(newColumn, apply(value, newDateFormat));
    }

    protected String apply(String from, DateTimeFormatter dateTimeFormatter) {
        try {
            LocalDateTime date = LocalDateTime.ofEpochSecond(Long.parseLong(from), 0, ZoneOffset.UTC);
            final String to = dateTimeFormatter.format(date);
            return to;
        } catch (NumberFormatException e) {
            return "";
        }
    }

    /**
     * Create the new "string length" column
     *
     * @param column the current column metadata
     * @return the new column metadata
     */
    private ColumnMetadata createNewColumn(final ColumnMetadata column) {
        return ColumnMetadata.Builder //
                .column() //
                .name(column.getName() + APPENDIX) //
                .type(Type.DATE) //
                .headerSize(column.getHeaderSize()) //
                .build();
    }

}
