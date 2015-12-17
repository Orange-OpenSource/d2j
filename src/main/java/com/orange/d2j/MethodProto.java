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

import java.util.List;

import soot.Type;

/**
 * Method prototype
 * @author Pierre Cregut
 *
 */
public class MethodProto {
	
	/**
	 * Type returned by the method
	 */
	final public Type ret;
	
	/**
	 * List of parameters 
	 */
	final public List<Type> params;

	/**
	 * Constructor (non mutable tuple).
	 * @param params
	 * @param ret
	 */
	public MethodProto(List <Type> params, Type ret) {
		this.params = params;
		this.ret = ret;
	}
}
