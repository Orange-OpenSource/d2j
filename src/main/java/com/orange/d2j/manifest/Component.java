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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;

/**
 * @author piac6784
 * Abstraction of a component in the manifest
 */
public class Component {
	
	private static final String ATT_ICON = "icon";
	private static final String ATT_LABEL = "label";
	private static final String ATT_NAME = "name";
	private static final String ATT_PERMISSION = "permission";
	private static final String ATT_ENABLED = "enabled";
	private static final String ATT_EXPORTED = "exported";
	
	protected List <Filter> filters = new ArrayList<Filter>();
	protected Map<String, MetaData> metaData = new HashMap<String,MetaData>(); 
	/**
	 * Icon
	 */
	public final String icon;
	/**
	 * Label
	 */
	public final String label;
	/**
	 * Name
	 */
	public final String name;
	/**
	 * Permission required (or null)
	 */
	public final String permission;
	/**
	 * Component is exported
	 */
	public final TSBoolean exported;
	/**
	 * Component is enabled
	 */
	public final TSBoolean enabled;

	/**
	 * Component constructor
	 * @param attributes
	 */
	public Component(Attributes attributes) {
		icon = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ICON);
		label = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_LABEL);
		name =  attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_NAME);
		permission=  attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PERMISSION);
		exported =  boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_EXPORTED));
		enabled =  boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ENABLED));
	}

	/**
	 * Add a new filter to the definition
	 * @param filter
	 */
	public void add(Filter filter) { filters.add(filter); }
	
	/**
	 * Adds new meta data
	 * @param key
	 * @param value
	 * @param isResource
	 */
	public void addMetaDataValue(String key, String value, boolean isResource) {
		metaData.put(key, new MetaData(value, isResource));
	}
	
	/**
	 * Parse a boolean or gives back null.
	 * @param value
	 * @return
	 */
	static TSBoolean boolOfString(String value) {
		if (value == null) return TSBoolean.UNDEFINED;
		try { return Boolean.valueOf(value) ? TSBoolean.TRUE : TSBoolean.FALSE; } catch (Exception e) { return TSBoolean.UNDEFINED; }
	}
	
	/**
	 * Parse an integer or gives back null
	 * @param value
	 * @return
	 */
	static Integer intOfString(String value) {
		if (value == null) return null;
		try { return Integer.valueOf(value); } catch (NumberFormatException e) { return null; }
	}
	
	/**
	 * Parse a comma separated list in a string array. 
	 * @param value
	 * @return null if null otherwise the array.
	 */
	static String[] arrayOfString(String value) {
		if (value == null) return null;
		return value.split(",");
	}

	protected StringBuilder toStringBuilder(String string) {
		StringBuilder r = new StringBuilder();
		r.append("{"); r.append(string);
		r.append(" name="); r.append(name);
		if (label != null) { r.append(" label="); r.append(label); }
		if (icon != null) {r.append(" icon="); r.append(icon); }
		if (permission != null) {r.append(" permission="); r.append(permission); }
		if (enabled != null) {r.append(" enabled="); r.append(enabled); }
		if (exported != null) {r.append(" exported="); r.append(exported); }
		return r;
	}
	
	/**
	 * Get the list of filters
	 * @return
	 */
	public List<Filter> getFilters() { return filters; }
	
	/**
	 * Get the meta data associated.
	 * @return
	 */
	public Map<String,MetaData> getMetaDatas() { return metaData; }
}
