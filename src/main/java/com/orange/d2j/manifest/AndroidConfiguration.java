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

import com.orange.d2j.D2JLog;

/**
 * A configuration describes the input/output hardware required.
 * @author piac6784
 *
 */
public class AndroidConfiguration {
	/**
	 * Does the app requires the five navigation buttons
	 */
	final public boolean requiresFiveWayNav;
	/**
	 * Does the app requires a hardware keyboard.
	 */
	final public boolean requiresHardKeyboard;
	/**
	 * Keyboard type (numeric or full)
	 */
	final public String keyboardType;
	/**
	 * Navigation type
	 */
	final public String navigationType;
	/**
	 * Touchscreen type
	 */
	final public String touchScreenType;
	
	/**
	 * Constructor of the record.
	 * @param req5Way
	 * @param reqHardKB
	 * @param kbType
	 * @param navType
	 * @param touchscreenType2
	 */
	public AndroidConfiguration(boolean req5Way, boolean reqHardKB, String kbType,
			String navType, String touchscreenType2) {
		requiresFiveWayNav = req5Way;
		requiresHardKeyboard = reqHardKB;
		keyboardType = kbType;
		navigationType = navType;
		touchScreenType = touchscreenType2;
		
		D2JLog.severe("CONFIG "+ req5Way + "/" + reqHardKB + "/" + kbType + "/" + navType + "/" + touchscreenType2);
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		if (requiresFiveWayNav) buf.append(", 5Way Navigation");
		if (requiresHardKeyboard) buf.append(", Hard Keyboard");
		if (!keyboardType.equals(ManifestContentHandler.UNDEFINED)) {
			buf.append(", "); buf.append(keyboardType);
		}
		if (!navigationType.equals(ManifestContentHandler.UNDEFINED)) {
			buf.append(", "); buf.append(navigationType);
		}
		if (!touchScreenType.equals(ManifestContentHandler.UNDEFINED)) {
			buf.append(", "); buf.append(touchScreenType);
		}
		D2JLog.warning(buf.toString());
		String r = buf.toString();
		return (r.equals("") ? r : r.substring(2));
	}
}