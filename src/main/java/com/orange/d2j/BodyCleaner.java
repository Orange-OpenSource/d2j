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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.toolkits.typing.fast.BottomType;

/**
 * @author Pierre Cregut
 * 
 * This class cleans up a generated body after typing. It removes dead registers with
 * type bottom_type. If those registers are used, this corresponds to dead code that has
 * not been typed. We remove the statement.
 */

public class BodyCleaner {
	final private Body body;

	/**
	 * Constructor
	 * @param body
	 */
	public BodyCleaner(Body body) {
		this.body = body;
	}
	
	/**
	 * Perform the simplification. 
	 */
	public void cleanUp() {
		removeUnusedLocals();
		removeTooDeadStmt();
	}

	private void removeTooDeadStmt() {
		Iterator <Local> itLocals = body.getLocals().iterator();
		while (itLocals.hasNext()) {
			Local local = itLocals.next();
			if (local.getType() instanceof BottomType) itLocals.remove();
		}
	}

	@SuppressWarnings("unchecked")
	private void removeUnusedLocals() {
		PatchingChain <Unit> chain = body.getUnits();
		List <Unit> toRemove = new ArrayList <Unit>();
		for(Unit unit: chain) {
			List <ValueBox> contents = unit.getUseAndDefBoxes();
			for(ValueBox vb: contents) {
				Value v = vb.getValue();
				if(v instanceof Local && ((Local) v).getType() instanceof BottomType) {
					toRemove.add(unit);
					break;
				}
			}
		}
		chain.removeAll(toRemove);
	}

}
