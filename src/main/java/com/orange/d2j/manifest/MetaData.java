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

/**
 * @author piac6784
 * Meta Data Value.
 */
public class MetaData {
	
	/**
	 * Is the value in fact a resource.
	 */
	public final boolean isResource;
	
	/**
	 * The value of the meta-data element
	 */
	public final String value;
	
	/**
	 * Constructor
	 * @param value
	 * @param isResource
	 */
	public MetaData(String value, boolean isResource) { this.value = value; this.isResource=isResource; }
	
	@Override
	public String toString() { return "{" + (isResource ? "resource " : "value ") + value + "}"; }
}