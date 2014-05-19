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

import soot.ArrayType;
import soot.Local;
import soot.UnitPrinter;
import soot.Value;
import soot.jimple.internal.AbstractStmt;
import soot.util.Switch;

/**
 * Dummy instruction that must be expanded during the generation of the actual code.
 * It is used to represent Dalvick filled-new-array instructions.
 * @author piac6784
 *
 */
@SuppressWarnings("unchecked")
public final class FillArrayStmt extends AbstractStmt implements Cloneable {
    private static final long serialVersionUID = 1L;
    final private List<Value> contents;
	final private ArrayType type;
	private Local lhs;
	
	/**
	 * Tupple constructor.
	 * @param contents
	 * @param isNew
	 */
	public FillArrayStmt(Local lhs, List<Value> contents, ArrayType type) {
		this.lhs = lhs;
		this.contents = contents;
		this.type = type;
	}
	
	/**
	 * What is stored in the array
	 * @return
	 */
	public List<Value> getContents() { return contents; }

	/**
	 * Get the type of the array if new or null if only a fill array.
	 * @return
	 */
	public ArrayType getArrayType() { return type; }
	
	/**
	 * The value assigned to
	 * @return
	 */
	public Local getLhs() { return lhs; }
	
	@Override
	public Object clone() {	return new FillArrayStmt(lhs,contents,type); }

	@SuppressWarnings({"rawtypes"})
	@Override
	public List getUseBoxes() {	return null; }

	@Override
	public void toString(UnitPrinter arg) {
		arg.literal("DvkArrayFill" + contents);
	}

	@Override
	public void apply(Switch arg0) { }

	@Override
	public boolean branches() {	return false; }

	@Override
	public boolean fallsThrough() {	return false; }


}
