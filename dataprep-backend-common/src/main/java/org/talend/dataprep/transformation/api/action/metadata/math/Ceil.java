// ============================================================================
//
// Copyright (C) 2006-2015 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprep.transformation.api.action.metadata.math;

import org.springframework.stereotype.Component;
import org.talend.dataprep.transformation.api.action.metadata.ActionMetadata;

/**
 * This will compute the Returns the smallest (closest to negative infinity) value that is greater than or equal to the
 * value and is equal to a mathematical integer.
 * 
 * @see Math#ceil(double)
 */
@Component(Ceil.ACTION_BEAN_PREFIX + Ceil.CELL_ACTION_NAME)
public class Ceil extends AbstractRound {

    /** The action name. */
    public static final String CELL_ACTION_NAME = "ceil"; //$NON-NLS-1$

    /**
     * @see ActionMetadata#getName()
     */
    @Override
    public String getName() {
        return CELL_ACTION_NAME;
    }

    protected int compute(double from) {
        return (int) Math.ceil(from);
    }
}