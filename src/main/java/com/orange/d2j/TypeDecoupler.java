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
import java.util.List;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.Unit;
import soot.UnknownType;
import soot.jimple.DefinitionStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;

/**
 * This class modifies a jimple body that does not type as is to make
 * it acceptable by the typechecker.
 * It finds the variables that may be involved in a type weakening. It
 * introduces auxiliary variables that act as a buffer between the definitions
 * and the common use. 
 * @author piac6784
 *
 */
public class TypeDecoupler {
	private final Body body;
	private UnitGraph graph;
	private LocalDefs localDefs;
	private SimpleLiveLocals liveVars;
	private static int count = 0;
	List <ToInsert> todo = new ArrayList<ToInsert>();
	
	/**
	 * Constructor.
	 * @param body the body of the method to analyse.
	 */
	public TypeDecoupler(Body body) {
		this.body = body;
    	ReducedExceptionalUnitGraph unitgraph = new ReducedExceptionalUnitGraph(body);
        liveVars = new SimpleLiveLocals(unitgraph);
        localDefs = new SmartLocalDefs(unitgraph, liveVars);
        graph = unitgraph;
	}
	
	/**
	 * Description of a local for which we need to insert decoupling assingments.
	 * @author piac6784
	 *
	 */
	public static class ToInsert {
		private Local local;
		private Unit unit;
		private List<Unit> preds;

		/**
		 * Constructor 
		 * @param unit the receiving statement where all uses are concentrated.
		 * @param local the local to decouple
		 * @param preds the origins.
		 */
		public ToInsert(Unit unit, Local local, List <Unit> preds) {
			this.unit = unit;
			this.local = local;
			this.preds = preds;
		}
		
		/**
		 * Perform the insertion itself in the code. Must be done after reading the code or there
		 * will be a concurrent modification error raised.
		 * @param body
		 */
		public void insert(Body body) {
			PatchingChain <Unit> pc = body.getUnits();
			Local aux = new JimpleLocal("D" + count++, UnknownType.v());
			body.getLocals().addLast(aux);
			Stmt restoreStmt = new JAssignStmt(local, aux);
			if (unit instanceof DefinitionStmt) pc.insertAfter(restoreStmt, unit);
			else pc.insertBefore(restoreStmt, unit);
			for(Unit pred: preds) {
				Stmt saveStmt = new JAssignStmt(aux,local);
				if (isBranching(pred)) pc.insertBefore(saveStmt, pred);
				else pc.insertAfter(saveStmt, pred);
			}
		}
	}
	
	/**
	 * Analyse the method body to find what to decouple.
	 */
	@SuppressWarnings("unchecked")
	public void findLocals() {
		for (Unit unit : body.getUnits()) {
			// We are only interested in join points.
			List <Unit> preds = graph.getPredsOf(unit); 
			if (preds.size() <= 1) continue;
			if (D2JLog.typing) D2JLog.warning("CONSIDERING UNIT " + unit);
			List <Local> lives = liveVars.getLiveLocalsBefore(unit);
			
			for (Local live : lives) {
				if (D2JLog.typing) D2JLog.warning("  CONSIDERING LOCAL " + live);
				List<Unit> def2 = localDefs.getDefsOfAt(live,unit);
				if (def2 == null || def2.size() <= 1) continue;
				
				for(Unit pred : preds) {
					if (D2JLog.typing) D2JLog.warning("    CONSIDERING PREDS " + pred);
					List <Local> lives2 = liveVars.getLiveLocalsAfter(pred);
					if (!lives2.contains(live)) continue;
					if (D2JLog.typing) D2JLog.warning("       CHECK 1");
					todo.add(new ToInsert(unit,live,preds));
					break;
				}	
			}
		}
		for(ToInsert i: todo) i.insert(body);
	}


	private static boolean isBranching(Unit stmt) {
		return (stmt instanceof GotoStmt || stmt instanceof IfStmt || stmt instanceof LookupSwitchStmt || stmt instanceof TableSwitchStmt);
	}
}
