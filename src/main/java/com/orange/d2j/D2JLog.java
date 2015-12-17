/* Software Name : DalvikToJimple
 * Version : 1.0
 *
 * Copyright © 2010 France Télécom
 * All rights reserved.
 */
package com.orange.d2j;

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

import java.util.logging.Logger;

/**
 * Utility class maintaining a logger for the D2J library. It also maintain a set
 * of boolean variables used to determin if something must be logged or not.
 * @author Pierre Cregut
 *
 */
public class D2JLog {
	
	/**
	 * The name of the property containing the debug flags.
	 */
	private final static String D2J_DEBUG_KEY = "d2j.debug";
	/**
	 * The name of the global logger
	 */
	final public static String LOG_NAME = "D2JLog";
	
	/**
	 * The global logger.
	 */
	final public static Logger log = Logger.getLogger(LOG_NAME);
	
	/**
	 * Debug flag for string constant parsing
	 */
	static boolean stringPool = false;
	/**
	 * Debug flag for instruction parsing
	 */
	static boolean instructions = false;
	/**
	 * Debug flag for the class parsing
	 */
	static boolean classes = false;
	/**
	 * Debug flag for the typing 
	 */
	static boolean typing = false;
	/**
	 * Debug flag for the resource management
	 */
	static boolean resources =  false; 
	/**
	 * Debug flag for the resource management
	 */
	static boolean print = false;
	
	/**
	 * Debug flag for the local splitter (modified for Android).
	 */
	static boolean splitter = false;

	/**
	 * Debug flag for creating a dot file.
	 */	
	static boolean dot = false;	
	/**
	 * Critical bug, should stop.
	 * @param s
	 */
	public static void severe(String s) {	log.severe(s); }

	/**
	 * Non critical bug
	 * @param s
	 */
	public static void warning(String s) { 
		log.warning(s);
	}

	/**
	 * Just a reminder
	 * @param s
	 */
	public static void info(String s) { System.err.println(s); /* log.info(s); */ }
	
	/**
	 * Modifies the debug flags according to a specification coded as a set of letters.
	 * @param s the letters representing who should be debugged.
	 */
	public static void setDebug() {
		String s = System.getProperty(D2J_DEBUG_KEY, "");
		stringPool = (s.indexOf('s') != -1);
		classes = (s.indexOf('c') != - 1);
		instructions =  (s.indexOf('i') != - 1);
		typing = (s.indexOf('t') != - 1);
		resources = (s.indexOf('r') != - 1);
		print = (s.indexOf('p') != - 1);
	}

}
