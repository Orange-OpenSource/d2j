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
 * Representation of the applicatin declared in the manifest
 */
public class Application {
	private final static String ATT_BACKUP_AGENT = "backupAgent";
	private final static String ATT_ALLOW_TASK_REPARENTING = "allowTaskReparenting";
	private final static String ATT_DEBUGGABLE="debuggable";
	private final static String ATT_ENABLED = "enabled";
	private final static String ATT_HAS_CODE = "hasCode";
	private final static String ATT_HARDWARE_ACCELERATED = "hardwareAccelerated";
	private final static String ATT_ICON = "icon";
	private final static String ATT_KILL_AFTER_RESTORE = "killAfterRestore";
	private final static String ATT_LABEL = "label";
	private final static String ATT_LOGO = "logo";
	private final static String ATT_MANAGE_SPACE_ACTIVITY="manageSpaceActivity";
	private final static String ATT_NAME = "name";
	private final static String ATT_PERMISSION = "permission";
	private final static String ATT_PERSISTENT = "persistent";
	private final static String ATT_PROCESS = "process";
	private final static String ATT_RESTORE_ANY_VERSION = "restoreAnyVersion";
	private final static String ATT_TASK_AFFINITY = "taskAffinity";
	private final static String ATT_THEME = "theme";
	
	/**
	 * 
	 */
	public final String backupAgent;
	/**
	 * 
	 */
	public final TSBoolean allowTaskReparenting;
	/**
	 * 
	 */
	public final TSBoolean debuggable;
	/**
	 * 
	 */
	public final TSBoolean enabled;
	/**
	 * 
	 */
	public final TSBoolean hasCode;
	/**
	 * 
	 */
	public final TSBoolean hardwareAccelerated;
	/**
	 * 
	 */
	public final String icon;
	/**
	 * 
	 */
	public final TSBoolean killAfterRestore;
	/**
	 * 
	 */
	public final String label;
	/**
	 * 
	 */
	public final String logo;
	/**
	 * 
	 */
	public final String manageSpaceActivity;
	/**
	 * 
	 */
	public final String name;
	/**
	 * 
	 */
	public final String permission;
	/**
	 * 
	 */
	public final String persistent;
	/**
	 * 
	 */
	public final String process;
	/**
	 * 
	 */
	public final TSBoolean restoreAnyVersion;
	/**
	 * 
	 */
	public final String taskAffinity;
	/**
	 * 
	 */
	public final String theme;
	
	/**
	 * Constructor parsing from XML attributes.
	 * @param attributes
	 */
	public Application(Attributes attributes) {
		backupAgent = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_BACKUP_AGENT);
		allowTaskReparenting = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ALLOW_TASK_REPARENTING));
		debuggable = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_DEBUGGABLE));
		enabled = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ENABLED));
		hasCode = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_HAS_CODE));
		hardwareAccelerated = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_HARDWARE_ACCELERATED));
		killAfterRestore = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_KILL_AFTER_RESTORE));
		restoreAnyVersion = Component.boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_RESTORE_ANY_VERSION));
		icon = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ICON);
		label = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_LABEL);
		logo = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_LOGO);
		manageSpaceActivity = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_MANAGE_SPACE_ACTIVITY);
		name = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_NAME);
		permission = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PERMISSION);
		persistent = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PERSISTENT);
		process = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PROCESS);
		taskAffinity = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_TASK_AFFINITY);
		theme = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_THEME);
	}
	
	@Override
	public String toString() {
		StringBuilder r = new StringBuilder();
		r.append("{application ");
		r.append(" name="); r.append(name);
		if (label != null) { r.append(" label="); r.append(label); }
		if (icon != null) {r.append(" icon="); r.append(icon); }
		if (permission != null) {r.append(" permission="); r.append(permission); }
		if (enabled != null) {r.append(" enabled="); r.append(enabled); }
		if (backupAgent != null) {r.append(" backupAgent="); r.append(backupAgent); }
		if (allowTaskReparenting != null) {r.append(" allowTaskReparenting="); r.append(allowTaskReparenting); }
		if (debuggable != null) {r.append(" debuggable="); r.append(debuggable); }
		if (hasCode != null) {r.append(" hasCode="); r.append(hasCode); }
		if (hardwareAccelerated != null) {r.append(" hardwareAccelerated="); r.append(hardwareAccelerated); }
		if (killAfterRestore != null) {r.append(" killAfterRestore="); r.append(killAfterRestore); }
		if (restoreAnyVersion != null) {r.append(" restoreAnyVersion="); r.append(restoreAnyVersion); }
		if (logo != null) {r.append(" logo="); r.append(logo); }
		if (manageSpaceActivity != null) {r.append(" manageSpaceActivity="); r.append(manageSpaceActivity); }
		if (persistent != null) {r.append(" persistent="); r.append(persistent); }
		if (process != null) {r.append(" process="); r.append(process); }
		if (taskAffinity != null) {r.append(" taskAffinity="); r.append(taskAffinity); }
		if (theme != null) {r.append(" theme="); r.append(theme); }
		r.append("}");
		return r.toString();
	}
}
