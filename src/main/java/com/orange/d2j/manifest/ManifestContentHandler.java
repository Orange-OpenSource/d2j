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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



/**
 * This class analyse the contents of the manifest in order to isolate entry points in 
 * the classes.dex file.
 * @author piac6784
 *
 */
/**
 * @author piac6784
 *
 */
public class ManifestContentHandler extends  DefaultHandler {

	/**
	 * Constant for small screens
	 */
	public final static int SMALL_SCREEN = 1;
	/**
	 * Constant for normal screens
	 */
	public final static int NORMAL_SCREEN = 2;
	/**
	 * Constant for large screens.
	 */
	public final static int LARGE_SCREEN = 4;
	
	private String packageName = null;
	private String applicationClassname = null;
	private String sharedUserId = null;
	private Application application = null;
	private Instrumentation instrumentation = null;
	
	private String versionCode = null;
	private String versionName = null;
	
	private Map <String, Component> activities = new HashMap<String, Component>();
	private Map <String, Service> services = new HashMap<String, Service>();
	private Map <String, Provider> providers = new HashMap<String, Provider>();
	private Map <String, Receiver> receivers = new HashMap<String, Receiver>();
	private Map<String, MetaData> metaData = new HashMap<String,MetaData>(); 

	private Component current_component = null;
	private Filter current_filter = null;

	

	private int supportedScreens = SMALL_SCREEN | NORMAL_SCREEN | LARGE_SCREEN;
	private boolean anyDensity = true;
	private Set <AndroidConfiguration> configurations = new HashSet<AndroidConfiguration>	();
	private String glVersion = null;
	private Set <String> requiredFeatures = new HashSet<String>	();
	private Set <String> optionalFeatures = new HashSet<String> ();
	private int minSdk = -1;
	private int maxSdk = -1;
	private int targetSdk = -1;
	private Locator locator;
	private Set<String> usedPermissions = new HashSet<String>();
	private Set<String> requiredLibraries = new HashSet<String>();
	private Set<String> optionalLibraries = new HashSet<String>();

	final static String MANIFEST = "manifest";
	final static String ANDROID_URI = "http://schemas.android.com/apk/res/android";
	final static String ACTION= "action";
	final static String PACKAGE  = "package";
	final static String ACTIVITY = "activity";
	final static String APPLICATION = "application";
	final static String ACTIVITY_ALIAS = "activity-alias";
	final static String META_DATA = "meta-data";
	final static String SERVICE = "service";
	final static String PROVIDER = "provider";
	final static String RECEIVER = "receiver";
	final static String INSTRUMENTATION = "instrumentation";
	final static String INTENT_FILTER = "intent-filter";
	final static String CATEGORY = "category";
	final static String SUPPORT_SCREENS = "supports-screens";
	final static String USE_CONFIGURATION = "uses-configuration";
	final static String USE_FEATURE = "uses-feature";
	final static String USE_SDK = "uses-sdk";
	final static String USE_LIBRARY = "uses-library";
	final static String USE_PERMISSION = "uses-permission";
	final static String ATT_PROCESS = "process";
	final static String ATT_VERSION_CODE = "versionCode";
	final static String ATT_VERSION_NAME = "versionName";
	final static String ATT_SHARED_USER_ID = "sharedUserId";
	final static String ATT_GL_VERSION = "glEsVersion";
	final static String ATT_MIN_SDK = "minSdkVersion";
	final static String ATT_TGT_SDK = "targetSdkVersion";
	final static String ATT_MAX_SDK = "maxSdkVersion";
	final static String ATT_REQUIRED = "required";
	final static String ATT_RESOURCE = "resource";
	final static String ATT_VALUE = "value";
	final static String ATT_SMALL = "smallScreens";
	final static String ATT_NORMAL = "normalScreens";
	final static String ATT_LARGE = "largeScreens";
	final static String ATT_PRIORITY = "priority";
	final static String ATT_ANY_DENSITY = "anyDensity";
	final static String ATT_REQ_FIVE_WAY_NAV = "reqFiveWayNav";
	final static String ATT_REQ_HARD_KEYBOARD = "reqHardKeyboard";
	final static String ATT_REQ_KEYBOARD_TYPE = "reqKeyboardType";
	final static String ATT_REQ_NAVIGATION = "reqNavigation";
	final static String ATT_REQ_TOUCHSCREEN = "reqTouchScreen";
	final static String ATT_NAME = "name";
	final static String ATT_TARGET_ACTIVITY = "targetActivity";
	final static String ATT_MIME_TYPE = "mimeType";
	final static String ATT_SCHEME = "scheme";
	final static String ATT_HOST = "host";
	final static String ATT_PORT = "port";
	final static String ATT_PATH = "path";
	final static String ATT_PATH_PREFIX = "pathPrefix";
	final static String ATT_PATH_PATTERN = "pathPattern";
	final static String UNDEFINED = "undefined";
	final static String DATA = "data";
		
	@Override
	public void endElement(String uri, String name, String qname)
			throws SAXException {
		if (name.equals(ACTIVITY) || name.equals(ACTIVITY_ALIAS)) {
			current_component = null;
		} else if (name.equals(SERVICE)) {
			current_component = null;
		} else if (name.equals(PROVIDER)) {
			current_component = null;
		} else if (name.equals(RECEIVER)) {
			current_component = null;
		} else if (name.equals(INTENT_FILTER)) {
			if (current_component == null) System.err.println("AT " + locator.getLineNumber());
			else current_component.add(current_filter);
			current_filter = null;
		}
	}

	/**
	 * Cleanup the names. The rules is somewhat complex
	 */
	
	public String cleanup(String component) {
		if (component != null) {
			if(component.length() > 0 && component.charAt(0) == '.') 
				component = packageName + component;
			else if (!component.contains("."))
				component = packageName + "." + component;
		}
		return component;
	}
	
	private void setSupportedScreens (Attributes attributes) {
		String attr = attributes.getValue(ANDROID_URI, ATT_SMALL);
		supportedScreens = (supportedScreens & ~SMALL_SCREEN) | ((attr == null || ! attr.equals("false")) ? SMALL_SCREEN : 0);
		attr = attributes.getValue(ANDROID_URI,ATT_NORMAL);
		supportedScreens = (supportedScreens & ~NORMAL_SCREEN) | ((attr == null || ! attr.equals("false")) ? NORMAL_SCREEN : 0);
		attr = attributes.getValue(ANDROID_URI,ATT_LARGE);
		supportedScreens = (supportedScreens & ~LARGE_SCREEN) | ((attr == null || ! attr.equals("false")) ? LARGE_SCREEN : 0);
		attr = attributes.getValue(ANDROID_URI, ATT_ANY_DENSITY);
		anyDensity = (attr == null || ! attr.equals("false"));		
	}
	@Override
	public void startElement(String uri, String name, String arg2,
			Attributes attributes) throws SAXException {
		if (name.equals(MANIFEST)) {
			packageName = attributes.getValue(ANDROID_URI, PACKAGE);
			sharedUserId = attributes.getValue(ANDROID_URI, ATT_SHARED_USER_ID);
			versionCode = attributes.getValue(ANDROID_URI, ATT_VERSION_CODE);
			versionName = attributes.getValue(ANDROID_URI, ATT_VERSION_NAME);
		} else if (name.equals(SUPPORT_SCREENS)) {
			setSupportedScreens(attributes);
		} else if (name.equals(USE_CONFIGURATION)) {
			addConfiguration(attributes);
		} else if (name.equals(USE_FEATURE)) {
			addFeature(attributes);
		} else if (name.equals(USE_LIBRARY)) {
			addLibrary(attributes);
		} else if (name.equals(USE_SDK)) {
			setSdkRequirements(attributes);
		} else if (name.equals(USE_PERMISSION)) {
			usedPermissions.add(attributes.getValue(ANDROID_URI,ATT_NAME));
		} else if (name.equals(ACTIVITY)) {
			Activity activity = new Activity(attributes);
			activities.put(cleanup(activity.name), activity);
			current_component = activity;
		} else if (name.equals(ACTIVITY_ALIAS)) {
			ActivityAlias activity = new ActivityAlias(attributes);
			activities.put(cleanup(activity.targetActivity), activity);
			current_component = activity;
		} else if (name.equals(PROVIDER)) {
			Provider provider = new Provider(attributes);
			providers.put(cleanup(provider.name), provider);
			current_component = provider;
		} else if (name.equals(SERVICE)) {
			Service service = new Service(attributes);
			services.put(cleanup(service.name), service);
			current_component = service;
		} else if (name.equals(RECEIVER)) {
			Receiver receiver = new Receiver(attributes);
			receivers.put(cleanup(receiver.name), receiver);
			current_component = receiver;
		} else if (name.equals(APPLICATION)) {
			applicationClassname = cleanup(attributes.getValue(ANDROID_URI,ATT_NAME));
			application = new Application(attributes);
		} else if (name.equals(INSTRUMENTATION)) {
			instrumentation = new Instrumentation(attributes);
		} else if (name.equals(INTENT_FILTER)) {
			current_filter = new Filter();
			String attr = attributes.getValue(ANDROID_URI, ATT_PRIORITY);
			if (attr != null) current_filter.priority = Integer.valueOf(attr);
				
		} else if (name.equals(META_DATA)) {
			String keyMetaData = attributes.getValue(ANDROID_URI, ATT_NAME);
			String valueMetaData = attributes.getValue(ANDROID_URI, ATT_VALUE);
			boolean isResource = false;
			if (valueMetaData == null) {
				valueMetaData = attributes.getValue(ANDROID_URI, ATT_RESOURCE);
				isResource = true;
			}
			if (current_component == null) addMetaDataValue(keyMetaData,valueMetaData,false);
			else current_component.addMetaDataValue(keyMetaData,valueMetaData,isResource);
			
		} else if (name.equals(ACTION)) {
			// We have seen actions directly at the level of activity. Sounds weird.
			if (current_filter != null)	current_filter.actions.add(attributes.getValue(ANDROID_URI, ATT_NAME));
		} else if (name.equals(CATEGORY)) {
			if (current_filter != null) current_filter.category = attributes.getValue(ANDROID_URI, ATT_NAME);
		} else if (name.equals(DATA)) {
			if (current_filter != null) {
				current_filter.mimeType = attributes.getValue(ANDROID_URI, ATT_MIME_TYPE);
				current_filter.scheme =  attributes.getValue(ANDROID_URI, ATT_SCHEME);
				current_filter.host =  attributes.getValue(ANDROID_URI, ATT_HOST);
				current_filter.port =  attributes.getValue(ANDROID_URI, ATT_PORT);
				current_filter.path =  attributes.getValue(ANDROID_URI, ATT_PATH);
				current_filter.pathPrefix =  attributes.getValue(ANDROID_URI, ATT_PATH_PREFIX);
				current_filter.pathPattern =  attributes.getValue(ANDROID_URI, ATT_PATH_PATTERN);
			}
		}
	}

	private void setSdkRequirements(Attributes attributes) {
		String attr = attributes.getValue(ANDROID_URI, ATT_MIN_SDK);
		if (attr != null) minSdk = Integer.valueOf(attr);
		attr = attributes.getValue(ANDROID_URI, ATT_MAX_SDK);
		if (attr != null) maxSdk = Integer.valueOf(attr);
		attr = attributes.getValue(ANDROID_URI, ATT_TGT_SDK);
		if (attr != null) targetSdk = Integer.valueOf(attr);
	}

	private void addFeature(Attributes attributes) {
		String attr = attributes.getValue(ANDROID_URI, ATT_GL_VERSION);
		if (attr != null) glVersion = attr;
		attr =  attributes.getValue(ANDROID_URI, ATT_REQUIRED);
		boolean required = attr == null || !attr.equals("false");
		attr = attributes.getValue(ANDROID_URI, ATT_NAME);
		if (attr != null) {
			if (required) requiredFeatures.add(attr);
			else optionalFeatures.add(attr);
		}
	}

	private void addLibrary(Attributes attributes) {
		String attr =  attributes.getValue(ANDROID_URI, ATT_REQUIRED);
		boolean required = attr == null || !attr.equals("false");
		attr = attributes.getValue(ANDROID_URI, ATT_NAME);
		if (attr != null) {
			if (required) requiredLibraries.add(attr);
			else optionalLibraries.add(attr);
		}
	}

	private void addConfiguration(Attributes attributes) {
		String attr = attributes.getValue(ANDROID_URI, ATT_REQ_FIVE_WAY_NAV);
		boolean req5Way = (attr != null) && attr.equals("false");
		attr = attributes.getValue(ANDROID_URI, ATT_REQ_HARD_KEYBOARD);
		boolean reqHardKB = (attr != null) && attr.equals("false");

		attr = attributes.getValue(ANDROID_URI, ATT_REQ_KEYBOARD_TYPE);
		String kbType = UNDEFINED;
		if (attr != null) {
			switch(Integer.parseInt(attr)) {
			case 1:
				kbType = "No keyboard";
				break;				
			case 2:
				kbType = "Qwerty keyboard";
				break;
			case 3:
				kbType = "12 Key keyboard";
				break;
			default:
				kbType = UNDEFINED;
			}
		}

		attr = attributes.getValue(ANDROID_URI, ATT_REQ_NAVIGATION);
		String navType = UNDEFINED;
		if (attr != null) {
			switch(Integer.parseInt(attr)) {
			case 1:
				navType = "No nav";
				break;				
			case 2:
				navType = "DPad";
				break;
			case 3:
				navType = "TrackPad";
				break;
			case 4:
				navType = "Wheel";
				break;
			default:
				navType = UNDEFINED;
			}
		}
		
		attr = attributes.getValue(ANDROID_URI, ATT_REQ_TOUCHSCREEN);
		String touchscreenType = UNDEFINED;
		if (attr != null) {
			switch(Integer.parseInt(attr)) {
			case 1:
				touchscreenType = "No touchscreen";
				break;				
			case 2:
				touchscreenType = "Stylus";
				break;
			case 3:
				touchscreenType = "Finger";
				break;
			default:
				touchscreenType = UNDEFINED;
			}
		}
		configurations.add(new AndroidConfiguration(req5Way, reqHardKB, kbType, navType, touchscreenType));
	}

	 @Override
	public void setDocumentLocator(Locator locator) {
		 this.locator = locator;
	}
	 
	/**
	 * List of activities published by the application
	 * @return
	 */
	public Set<String> getActivities() {
		return activities.keySet();
	}
	
	/**
	 * Intent filters associated to an activity
	 * @return
	 */
	public List <Filter> getActivityFilters(String activity) { return activities.get(activity).filters; }
	
	 
	/**
	 * List of services published by the application
	 * @return
	 */
	public Set<String> getServices() {
		return services.keySet();
	}
	
	/**
	 * Intent filters associated to a service
	 * @return
	 */
	public List <Filter> getServiceFilters(String activity) { return services.get(activity).filters; }
	
	 
	/**
	 * List of content providers published by the application
	 * @return
	 */
	public Set<String> getProviders() {
		return providers.keySet();
	}
	
	/**
	 * List of event broadcast receivers published by the application
	 * @return
	 */
	public Set<String> getReceivers() {
		return receivers.keySet();
	}

	/**
	 * Intent filters associated to a receiver
	 * @return
	 */
	public List <Filter> getReceiverFilters(String activity) { return receivers.get(activity).filters; }

	/**
	 * Gives back true if supports any density (default)
	 * @return
	 */
	public boolean supportsAnyDensity() { return anyDensity; }
	
	/**
	 * Gives back a set of flags indicating supported screen sizes.
	 * @return
	 */
	public int supportedScreenSizes() { return supportedScreens; }
	
	/**
	 * Gives back the set of supported configurations.
	 * @return
	 */
	public Set<AndroidConfiguration> getConfigurations() { return configurations; }
	
	/**
	 * Gives back the set of required features.
	 * @return
	 */
	public Set <String> requiredFeatures() { return requiredFeatures; }
	/**
	 * Gives back the set of optional features.
	 * @return
	 */
	public Set <String> optionalFeatures() { return optionalFeatures; }
	
	
	/**
	 * Gives back the set of required libraries.
	 * @return
	 */
	public Set <String> requiredLibraries() { return requiredLibraries; }
	/**
	 * Gives back the set of optional libraries.
	 * @return
	 */
	public Set <String> optionalLibraries() { return optionalLibraries; }
	
	/**
	 * Gives back the supported OpenGL ES version
	 * @return
	 */
	public String glVersion() { return glVersion; }
	
	/**
	 * Gives back minimum Sdk requirement
	 * @return
	 */
	public int minSdk() { return minSdk; }
	
	/**
	 * Gives back maximum Sdk requirement
	 * @return
	 */
	public int maxSdk() { return maxSdk; }

	/**
	 * Gives back target Sdk requirement
	 * @return
	 */
	public int targetSdk() { return targetSdk; }
	
	/**
	 * Gives back used permissions.
	 * @return
	 */
	public Set<String> usedPermissions() { return usedPermissions; }
	
	/**
	 * Gives back the classname of the application
	 * @return usually null unless it is defined (rare).
	 */
	public String getApplicationClassName() { return applicationClassname; }
	
	/**
	 * Gives back the userId to use to be shared with other apps.
	 * @return usually null unless it is defined (rare).
	 */
	public String getSharedUserId() { return sharedUserId; }
	
	/**
	 * Gives back the application description.
	 * @return 
	 */
	public Application getApplication() { return application; }
	
	/**
	 * An integer coresponding to the version of the application.
	 * @return a positive integer or -1 if not set.
	 */
	public int getVersionCode() {
		if (versionCode == null) return -1;
		try {
			return Integer.parseInt(versionCode); 
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	
	/**
	 * A readable version name.
	 * @return
	 */
	public String getVersionName() { return versionName; }
	
	/**
	 * Get a defined activity
	 * @param name complete name
	 * @return either an Activity or an ActivityAlias object.
	 */
	public Component getActivity(String name) { return activities.get(name); }
	
	/**
	 * Get a defined service
	 * @param name complete name
	 * @return
	 */
	public Service getService(String name) { return services.get(name); }
	
	/**
	 * Get a defined receiver
	 * @param name complete name
	 * @return
	 */
	public Receiver getReceiver(String name) { return receivers.get(name); }
	
	/**
	 * Get a defined Content provider.
	 * @param name complete name
	 * @return
	 */
	public Provider getProvider(String name) { return providers.get(name); }
	
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
	 * Access an application meta-data element
	 * @param key
	 * @return
	 */
	public MetaData getMetaData(String key) {
		return metaData.get(key);
	}
	
	/**
	 * Get instrumentation information if available.
	 * @return
	 */
	public Instrumentation getInstrumentation() {
		return instrumentation;
	}
	
	@Override
	public String toString() {
		StringBuilder r = new StringBuilder();
		r.append(application);
		r.append("\n  "); r.append(metaData);
		r.append("\nACTIVITIES\n");
		r.append(activities);
		r.append("\nSERVICES\n");
		r.append(services);
		r.append("\nPROVIDERS\n");
		r.append(providers);
		r.append("\nRECEIVERS\n");
		r.append(receivers);
		return r.toString();
	}
}
