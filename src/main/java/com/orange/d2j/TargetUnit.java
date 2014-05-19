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


import soot.UnitPrinter;
import soot.jimple.internal.AbstractStmt;

/**
 * A placeholder statement for the target of a jump. It is then replaced with the correct statement
 * coresponding to the offset in the DEX file.
 * @author piac6784
 *
 */

@SuppressWarnings("unchecked")
public final class TargetUnit extends AbstractStmt implements Cloneable {
	/** Default serial ID */
    private static final long serialVersionUID = 1L;
    
    final private int position;
	
	/**
	 * Constructor 
	 * @param position offset in the dex file
	 */
	public TargetUnit(int position) {
		this.position = position;
	}
	
	/**
	 * Gives back the offset in the dex file
	 * @return
	 */
	public int getPosition() { return position; }

	@Override
	public Object clone() {
	    return new TargetUnit(position);
	}

	@Override
	public void toString(UnitPrinter arg) { arg.literal("<@" + position + ">"); }
	
	@Override
	public String toString() {return "<@" + position  + ">"; }

	@Override
	public boolean branches() { return false; }

	@Override
	public boolean fallsThrough() {	return false; }
	
}
