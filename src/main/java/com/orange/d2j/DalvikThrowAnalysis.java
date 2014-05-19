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

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.JastAddJ.ModExpr;
import soot.baf.ThrowInst;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.NegExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.ThrowStmt;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.exceptions.ThrowableSet;

/**
 * Creates a Soot Throw analysis that pretends that return cannot throw an exception. This is
 * the case if another analysis can check that the entermonitor exitmonitor are well paired.
 * This means that looking at the CFG reduced to entermonitor exitmonitor as a regexp, there is no unbalanced
 * word especially under a loop (star word). This analysis should be implemented.
 * @author piac6784
 *
 */
public class DalvikThrowAnalysis implements ThrowAnalysis {

	final private ThrowAnalysis encapsulated;


	/**
	 * Constructor that takes the ThrowAnalysis to wrap as argument
	 * @param encapsulated
	 */
	public DalvikThrowAnalysis(ThrowAnalysis encapsulated) {
		this.encapsulated = encapsulated;
	}

	@Override
	public ThrowableSet mightThrow(Unit u) {
		ThrowableSet result;
		ThrowableSet empty = ThrowableSet.Manager.v().EMPTY;
		if (u instanceof AssignStmt) {
			Value right = ((AssignStmt) u).getRightOp();
			Value left = ((AssignStmt) u).getLeftOp(); 
			if (left instanceof Local && 
				(right instanceof Local || right instanceof Constant 
				 || (right instanceof BinopExpr && !(right instanceof DivExpr || right instanceof ModExpr))		
				 || right instanceof NegExpr))
				result = empty;
			else result = encapsulated.mightThrow(u);
		}
		else if (u instanceof ReturnStmt || u instanceof ReturnVoidStmt || u instanceof GotoStmt || u instanceof DefinitionStmt || u instanceof IfStmt ) result = empty;
		else result = encapsulated.mightThrow(u);
		if (D2JLog.typing) D2JLog.info("MIGHT THROW " + u + ": " + result.toAbbreviatedString());
		return result;
	}

	@Override
	public ThrowableSet mightThrowExplicitly(ThrowInst inst) {
		return encapsulated.mightThrowExplicitly(inst);
	}

	@Override
	public ThrowableSet mightThrowExplicitly(ThrowStmt stmt) {
		return encapsulated.mightThrowExplicitly(stmt);
	}

	@Override
	public ThrowableSet mightThrowImplicitly(ThrowInst inst) {
		return encapsulated.mightThrowImplicitly(inst);
	}

	@Override
	public ThrowableSet mightThrowImplicitly(ThrowStmt stmt) {
		return encapsulated.mightThrowImplicitly(stmt);
	}

}
