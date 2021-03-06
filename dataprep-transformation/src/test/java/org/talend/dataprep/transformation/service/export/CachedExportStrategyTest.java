// ============================================================================
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.service.export;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.talend.dataprep.api.export.ExportParameters.SourceType.FILTER;
import static org.talend.dataprep.api.export.ExportParameters.SourceType.HEAD;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.talend.ServiceBaseTest;
import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.export.ExportParameters;
import org.talend.dataprep.api.export.ExportParameters.SourceType;
import org.talend.dataprep.api.preparation.Preparation;
import org.talend.dataprep.cache.ContentCache;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.dataprep.cache.CacheKeyGenerator;
import org.talend.dataprep.cache.TransformationCacheKey;

public class CachedExportStrategyTest extends ServiceBaseTest {

    @Autowired
    ContentCache cache;

    @Autowired
    CacheKeyGenerator cacheKeyGenerator;

    @Autowired
    CachedExportStrategy cachedExportStrategy;

    @Autowired
    PreparationRepository preparationRepository;

    @Before
    public void setUp() {
        super.setUp();
        final Preparation preparation = new Preparation("1234", "1.0");
        preparation.setDataSetId("1234");
        preparation.setHeadId("0");
        preparationRepository.add(preparation);

        final TransformationCacheKey cacheKey =
                cacheKeyGenerator.generateContentKey("1234", "1234", "0", "text", HEAD, "");
        putKeyInCache(cacheKey);
        final TransformationCacheKey filteredSampleCacheKey =
                cacheKeyGenerator.generateContentKey("1234", "1234", "0", "text", FILTER, "");
        putKeyInCache(filteredSampleCacheKey);
    }

    private void putKeyInCache(TransformationCacheKey cacheKey) {
        try (OutputStream text = cache.put(cacheKey, ContentCache.TimeToLive.DEFAULT)) {
            text.write("{}".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doTestAcceptShouldPassIfCacheEntryExistsFrom(SourceType from) throws Exception {
        // Given
        final ExportParameters parameters = new ExportParameters();
        parameters.setDatasetId("1234");
        parameters.setPreparationId("1234");
        parameters.setStepId("0");
        parameters.setExportType("text");
        parameters.setFrom(from);

        // Then
        assertTrue(cachedExportStrategy.test(parameters));
    }

    @Test
    public void shouldAcceptIfCacheEntryExistsFromHEAD() throws Exception {
        doTestAcceptShouldPassIfCacheEntryExistsFrom(HEAD);
    }

    @Test
    public void shouldAcceptIfCacheEntryExistsFromFILTER() throws Exception {
        doTestAcceptShouldPassIfCacheEntryExistsFrom(FILTER);
    }

    @Test
    public void shouldNotAcceptIfCacheEntryDoesNotExists() throws Exception {
        // Given
        final ExportParameters parameters = new ExportParameters();
        parameters.setDatasetId("1234");
        parameters.setPreparationId("2345"); // Preparation differs from key.
        parameters.setStepId("0");
        parameters.setExportType("text");
        parameters.setFrom(HEAD);

        // Then
        assertFalse(cachedExportStrategy.test(parameters));
    }

    @Test
    public void shouldNotAcceptNullParameter() throws Exception {
        // Then
        assertFalse(cachedExportStrategy.test(null));
    }

    @Test
    public void shouldNotAcceptWhenParameterContentIsPresent() throws Exception {
        // Then
        final ExportParameters parameters = new ExportParameters();
        parameters.setContent(new DataSet());
        assertFalse(cachedExportStrategy.test(parameters));
    }

    @Test
    public void shouldNotAcceptNullPreparationId() throws Exception {
        // Then
        final ExportParameters parameters = new ExportParameters();
        parameters.setContent(new DataSet());
        parameters.setPreparationId(null);
        assertFalse(cachedExportStrategy.test(parameters));
    }

}
