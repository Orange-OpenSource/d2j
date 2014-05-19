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
 * @author piac6784
 * Abstraction of a service
 */
public class Service extends Component {
	final private static String ATT_PROCESS = "process";
	
	/**
	 * Process name for share user id 
	 */
	public final String process;

	/**
	 * Constructor
	 * @param attributes
	 */
	public Service(Attributes attributes) { 
		super(attributes);
		process = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PROCESS);
	}

	@Override
	public String toString() {
		StringBuilder r = toStringBuilder("service");
		if (process != null) { r.append(" process="); r.append(process); }
		r.append("\n  "); r.append(metaData);
		r.append("\n  "); r.append(filters);
		r.append("}\n");
		return r.toString();
	}

}
