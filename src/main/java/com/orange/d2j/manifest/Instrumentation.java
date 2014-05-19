/* Software Name : DalvikToJimple
 * Version : 1.0
 *
 * Copyright © 2010 France Télécom
 * All rights reserved.
 */
package com.orange.d2j.manifest;

/*
 * #%L
 * D2J
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2009 - 2014 Orange SA
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import org.xml.sax.Attributes;

/**
 * The Class Instrumentation.
 *
 * @author piac6784
 * Represents instrumentation information on an APK
 */
public class Instrumentation {
	
	/** The Constant ATT_HANDLE_PROFILING. */
	private static final String ATT_HANDLE_PROFILING = "handleProfiling";
	
	/** The Constant ATT_FUNCTIONAL_TEST. */
	private static final String ATT_FUNCTIONAL_TEST = "functionalTest";
	
	/** The Constant ATT_TARGET_PACKAGE. */
	private static final String ATT_TARGET_PACKAGE = "targetPackage";
	
	/** The Constant ATT_ICON. */
	private static final String ATT_ICON = "icon";
	
	/** The Constant ATT_LABEL. */
	private static final String ATT_LABEL = "label";
	
	/** The Constant ATT_NAME. */
	private static final String ATT_NAME = "name";
	
	/** Support functional tests ?. */
	private final TSBoolean functionalTest;
	
	/** Support profiling. */
	private final TSBoolean handleProfiling;
	
	/** Icon. */
	private final String icon;
	
	/** Label. */
	private final String label;
	
	/** name. */
	private final String name;
	
	/** Package targeted. */
	private final String targetPackage;
	
	/**
	 * Constructor parsing from XML attributes.
	 *
	 * @param attributes the attributes
	 */
	public Instrumentation(Attributes attributes) {
		targetPackage = attributes.getValue(ManifestContentHandler.ANDROID_URI,ATT_TARGET_PACKAGE);
		icon = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ICON);
		label = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_LABEL);
		name =  attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_NAME);
		functionalTest = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_FUNCTIONAL_TEST));
		handleProfiling= Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_HANDLE_PROFILING));

	}

    /**
     * Gets the functional test.
     *
     * @return the functional test
     */
    public TSBoolean getFunctionalTest() {
        return functionalTest;
    }

    /**
     * Gets the handle profiling.
     *
     * @return the handle profiling
     */
    public TSBoolean isHandlingProfiling() {
        return handleProfiling;
    }

    /**
     * Gets the icon.
     *
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the target package.
     *
     * @return the target package
     */
    public String getTargetPackage() {
        return targetPackage;
    }
}
