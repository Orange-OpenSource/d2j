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

import java.util.Arrays;

import org.xml.sax.Attributes;

/**
 * @author Pierre Cregut
 * Abstraction of a content provider in the manifest.
 */
public class Provider extends Component {
	private final static String ATT_AUTHORITIES = "authorities";
	private final static String ATT_GRANT_URI_PERMISSIONS = "grantUriPermissions";
	private final static String ATT_INIT_ORDER = "initOrder";
	private final static String ATT_MULTI_PROCESS = "multiProcess";
	private final static String ATT_PROCESS = "process";
	private final static String ATT_READ_PERMISSION = "readPermission";
	private final static String ATT_WRITE_PERMISSION = "writePermission";
	private final static String ATT_SYNCABLE = "syncable";
	
	/**
	 * authorities for the provider
	 */
	public final String [] authorities;
	/**
	 * grant URI
	 */
	public final TSBoolean grantUriPermissions;
	/**
	 * 
	 */
	public final Integer initOrder;
	/**
	 * 
	 */
	public final TSBoolean multiProcess;
	/**
	 * process for shared user id
	 */
	public final String process;
	/**
	 * distinct read permission
	 */
	public final String readPermission;
	/**
	 * distinct write permission
	 */
	public final String writePermission;
	/**
	 * syncable provider
	 */
	public final TSBoolean syncable;
	
	/**
	 * Constructor. Parse attributes.
	 * @param attributes
	 */
	public Provider(Attributes attributes) {
		super(attributes);
		authorities = arrayOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_AUTHORITIES));
		grantUriPermissions = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_GRANT_URI_PERMISSIONS));
		initOrder = intOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_INIT_ORDER));
		multiProcess = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_MULTI_PROCESS));
		process = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PROCESS);
		readPermission = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_READ_PERMISSION);
		writePermission = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_WRITE_PERMISSION);
		syncable = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_SYNCABLE));
	}
	
	@Override
	public String toString() {
		StringBuilder r = toStringBuilder("provider");
		if (authorities != null) { r.append(" authorities="); r.append(Arrays.toString(authorities)); }
		if (grantUriPermissions != null) { r.append(" grantUriPermissions="); r.append(grantUriPermissions); }
		if (initOrder != null) { r.append(" initOrder="); r.append(initOrder); }
		if (multiProcess != null) { r.append(" multiProcess="); r.append(multiProcess); }
		if (process != null) { r.append(" process="); r.append(process); }
		if (readPermission != null) { r.append(" readPermission="); r.append(readPermission); }
		if (writePermission != null) { r.append(" writePermission="); r.append(writePermission); }
		if (syncable != null) { r.append(" syncable="); r.append(syncable); }
		r.append("\n  "); r.append(metaData);
		r.append("}\n");
		return r.toString();
	}
}
