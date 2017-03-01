package org.talend.dataprep.transformation.api.action.metadata.date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.talend.dataprep.api.dataset.ColumnMetadata.Builder.column;
import static org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils.getColumn;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.api.type.Type;
import org.talend.dataprep.transformation.api.action.DataSetRowAction;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.api.action.metadata.ActionMetadataTestUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit test for the ChangeDatePattern action.
 * 
 * @see ChangeDatePattern
 */
public class ChangeDatePatternTest {

    /** The row consumer to test. */
    private DataSetRowAction rowClosure;

    /** The action to test. */
    private ChangeDatePattern action;

    /**
     * Constructor.
     */
    public ChangeDatePatternTest() throws IOException {
        action = new ChangeDatePattern();

        Map<String, String> parameters = ActionMetadataTestUtils.parseParameters( //
                action, //
                ChangeDatePatternTest.class.getResourceAsStream("changeDatePatternAction.json"));

        final Action action = this.action.create(parameters);
        rowClosure = action.getRowAction();
    }

    private static void setStatistics(DataSetRow row, String columnId, InputStream statisticsContent) throws IOException {
        String statistics = IOUtils.toString(statisticsContent);
        row.getRowMetadata().getById(columnId).setStatistics(statistics);
    }

    @Test
    public void testAdapt() throws Exception {
        assertThat(action.adapt(null), is(action));
        ColumnMetadata column = column().name("myColumn").id(0).type(Type.STRING).build();
        assertThat(action.adapt(column), is(action));
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_check_column_id_parameter_when_dealing_with_row_metadata() {
        action.create(new HashMap<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_check_new_pattern_parameter_when_dealing_with_row_metadata() {
        Map<String, String> missingParameters = new HashMap<>();
        missingParameters.put(ChangeDatePattern.COLUMN_ID, "0000");
        missingParameters.put(ChangeDatePattern.NEW_PATTERN, "toto");
        action.create(missingParameters);
    }

    @Test
    public void should_change_column_metadata() throws IOException {
        // given
        String statistics = IOUtils.toString(ChangeDatePatternTest.class.getResourceAsStream("statistics_yyyy-MM-dd.json"));
        ColumnMetadata column = ColumnMetadata.Builder.column().id(1).name("due_date").statistics(statistics).type(Type.DATE)
                .build();
        RowMetadata rowMetadata = new RowMetadata(Collections.singletonList(column));

        // when
        TransformationContext context = new TransformationContext();
        DataSetRow actual = rowClosure.apply(new DataSetRow(rowMetadata), context);

        // then
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());
        JsonNode rootNode = mapper.readTree(column.getStatistics());
        String actualPattern = rootNode.get("patternFrequencyTable").get(0).get("pattern").textValue();
        assertEquals("dd - MMM - yyyy", actualPattern);
    }

    @Test
    public void toto() {
        action.getItems();
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_check_column_id_when_dealing_with_row() {
        action.create(new HashMap<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_check_new_pattern_parameter_when_dealing_with_row() {
        Map<String, String> insufficientParams = new HashMap<>();
        insufficientParams.put(ChangeDatePattern.COLUMN_ID, "0000");
        action.create(insufficientParams);
    }

    @Test
    public void should_process_row() throws Exception {

        // given
        Map<String, String> values = new HashMap<>();
        values.put("0000", "toto");
        values.put("0001", "04/25/1999");
        values.put("0002", "tata");
        DataSetRow row = new DataSetRow(values);
        setStatistics(row, "0001", ChangeDatePatternTest.class.getResourceAsStream("statistics_MM_dd_yyyy.json"));

        TransformationContext context = new TransformationContext();

        // when
        row = rowClosure.apply(row, context);

        // then
        Map<String, String> expectedValues = new HashMap<>();
        expectedValues.put("0000", "toto");
        expectedValues.put("0001", "25 - Apr - 1999");
        expectedValues.put("0002", "tata");

        assertEquals(expectedValues, row.values());
    }

    @Test
    public void should_process_row_when_value_does_not_match_pattern() throws Exception {
        // given
        Map<String, String> values = new HashMap<>();
        values.put("0000", "toto");
        values.put("0001", "05.28.99");
        values.put("0002", "tata");
        DataSetRow row = new DataSetRow(values);
        setStatistics(row, "0001", ChangeDatePatternTest.class.getResourceAsStream("statistics_MM_dd_yyyy.json"));

        TransformationContext context = new TransformationContext();

        // when
        row = rowClosure.apply(row, context);

        // then (values should be unchanged)
        assertEquals(values, row.values());
    }

    @Test
    public void should_process_row_when_value_is_empty() throws Exception {
        // given
        Map<String, String> values = new HashMap<>();
        values.put("0000", "toto");
        values.put("0001", "");
        values.put("0002", "tata");
        DataSetRow row = new DataSetRow(values);
        setStatistics(row, "0001", ChangeDatePatternTest.class.getResourceAsStream("statistics_MM_dd_yyyy.json"));

        TransformationContext context = new TransformationContext();

        // when
        row = rowClosure.apply(row, context);

        // then (values should be unchanged)
        assertEquals(values, row.values());
    }

    @Test
    public void should_accept_column() {
        assertTrue(action.accept(getColumn(Type.DATE)));
    }

    @Test
    public void should_not_accept_column() {
        assertFalse(action.accept(getColumn(Type.NUMERIC)));
        assertFalse(action.accept(getColumn(Type.FLOAT)));
        assertFalse(action.accept(getColumn(Type.STRING)));
        assertFalse(action.accept(getColumn(Type.BOOLEAN)));
    }
}