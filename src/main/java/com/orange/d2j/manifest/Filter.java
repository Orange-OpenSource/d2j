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

import java.util.HashSet;
import java.util.Set;

/**
 * Representation for an intent filter.
 * @author piac6784
 *
 */
public class Filter {
	/**
	 * filtered actions
	 */
	public Set <String> actions = new HashSet <String> ();
	/**
	 * category
	 */
	public String category = null;
	/**
	 * mime type for intent.
	 */
	public String mimeType = null;
	/**
	 * uri scheme
	 */
	public String scheme = null;
	/**
	 * host
	 */
	public String host = null;
	/**
	 * port
	 */
	public String port = null;
	/**
	 * exact path
	 */
	public String path = null;
	/**
	 * regexp for matching path
	 */
	public String pathPattern = null;
	/**
	 * prefix for matching paths
	 */
	public String pathPrefix = null;
	/**
	 * Priority of the filter
	 */
	public int priority = 0;
	
	@Override
	public String toString() {
		return "{Filter:" +
		(actions == null ? "" : ("actions=" + actions)) +
		(category == null ? "" : ("category=" + category)) + 
		(mimeType == null ? "" : (" mimeType=" + mimeType)) + 
		(scheme == null ? "" : (" scheme=" + scheme)) + 
		(host == null ? "" : (" host=" + host))+
		(port == null ? "" : (" port=" + port)) +
		(path == null ? "" : (" path=" + path)) + 
		(pathPattern == null ? "" : (" pathPattern=" + pathPattern)) +
		(pathPrefix == null ? "" : (" pathPrefix=" + pathPrefix)) +
		(priority == 0 ? "" : (" priority=" + priority)) + "}";
	}
}