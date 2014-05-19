
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

import java.util.Iterator;
import java.util.List;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.DefinitionStmt;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.StmtBody;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;

/**
 * The Class ConstantPropagator.
 */
public class ConstantPropagator {

    /**
     * Folds the constants in a body
     * 
     * @param b the body
     */
    public static void transform(StmtBody stmtBody) {
        ExceptionalUnitGraph unitGraph = new ExceptionalUnitGraph(stmtBody);
        LocalDefs localDefs = new SmartLocalDefs(unitGraph, new SimpleLiveLocals(unitGraph));
        // Get statements in use order.
        Iterator<Unit> stmtIt = (new PseudoTopologicalOrderer<Unit>()).newList(unitGraph, false)
                .iterator();
        while (stmtIt.hasNext()) {
            Unit stmt = stmtIt.next();
            Iterator<ValueBox> useBoxIt = stmt.getUseBoxes().iterator();
            while (useBoxIt.hasNext()) {
                ValueBox useBox = useBoxIt.next();
                Value use = useBox.getValue();
                if (!(use instanceof Local))
                    continue;
                Local local = (Local) use;
                List<Unit> defsOfUse = localDefs.getDefsOfAt(local, stmt);
                if (defsOfUse.size() != 1)
                    continue;
                Value defStmtRightOp = ((DefinitionStmt) defsOfUse.get(0)).getRightOp();
                if (((defStmtRightOp instanceof NumericConstant || defStmtRightOp instanceof NullConstant))
                        && useBox.canContainValue(defStmtRightOp))
                    useBox.setValue(defStmtRightOp);
            }
        } // optimizeConstants
    }

}
