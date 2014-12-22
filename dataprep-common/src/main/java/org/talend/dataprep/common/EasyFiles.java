// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.dataprep.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * created by stef on Dec 11, 2014 Detailled comment
 *
 */
public class EasyFiles {

    public static File getFile(String path) throws FileNotFoundException {
        URL resource = EasyFiles.class.getClassLoader().getResource(path);
        if (resource != null) {
            return new File(resource.getFile());
        } else {
            throw new FileNotFoundException();
        }
    }
}
