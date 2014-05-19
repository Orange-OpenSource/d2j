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
 * @author piac6784
 * Abstraction of an activity in manifest
 */
public class Activity extends Component {
	final private static String ATT_ALLOW_TASK_REPARENTING = "allowTaskReparenting";
	final private static String ATT_ALWAYS_RETAIN_TASK_STATE = "alwaysRetainTaskState";
	final private static String ATT_CLEAR_ON_LAUNCH = "clearOnLaunch";
	final private static String ATT_EXCLUDE_FROM_RECENT = "excludeFromRecent";
	final private static String ATT_FINISH_ON_TASK_LAUNCH = "finishOnTaskLaunch";
	final private static String ATT_HARDWARE_ACCELERATED = "hardwareAccelerated";
	final private static String ATT_STATE_NOT_NEEDED = "stateNotNeeded";
	
	final private static String ATT_CONFIG_CHANGES = "configChanges";
	final private static String ATT_SCREEN_ORIENTATION = "screenOrientation";
	final private static String ATT_PROCESS = "process";
	final private static String ATT_LAUNCH_MODE = "launchMode";
	final private static String ATT_TASK_AFFINITY = "taskAffinity";
	final private static String ATT_WINDOWS_SOFT_INPUT_MODE = "windowsSoftInputMode";

	/**
	 * allow parent change
	 */
	public final TSBoolean allowTaskReparenting;
	/**
	 * retain the task state
	 */
	public final TSBoolean alwaysRetainTaskState;
	/**
	 * clear on launch
	 */
	public final TSBoolean clearOnLaunch;
	/**
	 * Do not keep in recent task
	 */
	public final TSBoolean excludeFromRecent;
	/**
	 * finish on task launch
	 */
	public final TSBoolean finishOnTaskLaunch;
	/**
	 * Use hardware acceleration for display 
	 */
	public final TSBoolean hardwareAccelerated;
	/**
	 * state useless
	 */
	public final TSBoolean stateNotNeeded;

	/**
	 * Config changes handled
	 */
	public String[] configChanges;
	/**
	 * Screen orientation
	 */
	public String screenOrientation = "unspecified";
	/**
	 * Process name for shared process id
	 */
	public String process;
	/**
	 * Launch mode
	 */
	public String launchMode;
	/**
	 * task affinity
	 */
	public String taskAffinity;
	/**
	 * Windows soft input mode
	 */
	public String windowSoftInputMode;

	/**
	 * Constructor parsing attributes
	 * @param attributes
	 */
	public Activity(Attributes attributes) {
		super(attributes);
		allowTaskReparenting = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ALLOW_TASK_REPARENTING));
		alwaysRetainTaskState = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_ALWAYS_RETAIN_TASK_STATE));
		clearOnLaunch = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_CLEAR_ON_LAUNCH));
		excludeFromRecent = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_EXCLUDE_FROM_RECENT));
		finishOnTaskLaunch = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_FINISH_ON_TASK_LAUNCH));
		hardwareAccelerated = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_HARDWARE_ACCELERATED));
		stateNotNeeded = boolOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_STATE_NOT_NEEDED));
		
		configChanges = arrayOfString(attributes.getValue(ManifestContentHandler.ANDROID_URI , ATT_CONFIG_CHANGES));
		screenOrientation = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_SCREEN_ORIENTATION);
		process = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_PROCESS);
		launchMode = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_LAUNCH_MODE);
		taskAffinity = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_TASK_AFFINITY);
		windowSoftInputMode = attributes.getValue(ManifestContentHandler.ANDROID_URI, ATT_WINDOWS_SOFT_INPUT_MODE);
	}

	@Override
	public String toString() {
		StringBuilder r = toStringBuilder("activity");
		if (allowTaskReparenting != null) { r.append(" allowTaskReparenting="); r.append(allowTaskReparenting); }
		if (alwaysRetainTaskState != null) { r.append(" alwaysRetainTaskState="); r.append(alwaysRetainTaskState); }
		if (clearOnLaunch != null) { r.append(" clearOnLaunch="); r.append(clearOnLaunch); }
		if (excludeFromRecent != null) { r.append(" excludeFromRecent="); r.append(excludeFromRecent); }
		if (finishOnTaskLaunch != null) { r.append(" finishOnTaskLaunch="); r.append(finishOnTaskLaunch); }
		if (hardwareAccelerated != null) { r.append(" hardwareAccelerated="); r.append(hardwareAccelerated); }
		if (stateNotNeeded != null) { r.append(" stateNotNeeded="); r.append(stateNotNeeded); }
		if (configChanges != null) { r.append(" configChanges="); r.append(Arrays.toString(configChanges)); }
		if (screenOrientation != null) { r.append(" screenOrientation="); r.append(screenOrientation); }
		if (process != null) { r.append(" process="); r.append(process); }
		if (launchMode != null) { r.append(" launchMode="); r.append(launchMode); }
		if (taskAffinity != null) { r.append(" taskAffinity="); r.append(taskAffinity); }
		if (windowSoftInputMode != null) { r.append(" windowSoftInputMode="); r.append(windowSoftInputMode); }
		r.append("\n  "); r.append(metaData);
		r.append("\n  "); r.append(filters);
		r.append("}\n");
		return r.toString();
		
	}

	
}
