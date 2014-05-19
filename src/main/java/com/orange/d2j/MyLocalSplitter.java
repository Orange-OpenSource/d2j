
/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.BodyTransformer;
import soot.G;
import soot.Local;
import soot.Timers;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.options.Options;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;
import soot.toolkits.scalar.LocalDefs;
import soot.toolkits.scalar.LocalPacker;
import soot.toolkits.scalar.LocalUses;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;
import soot.util.Chain;

/**
 *    A BodyTransformer that attemps to indentify and separate uses of a local
 *    varible that are independent of each other. Conceptually the inverse transform
 *    with respect to the LocalPacker transform.
 *
 *    For example the code:
 *
 *    for(int i; i < k; i++);
 *    for(int i; i < k; i++);
 *
 *    would be transformed into:
 *    for(int i; i < k; i++);
 *    for(int j; j < k; j++);
 *
 *
 *    @see BodyTransformer
 *    @see LocalPacker
 *    @see Body 
 */
public class MyLocalSplitter extends BodyTransformer
{
	static MyLocalSplitter privCell = null;
    private MyLocalSplitter() {}
    
    /**
     * Access to the unique instance.
     * @return
     */
    public static synchronized MyLocalSplitter v() { if (privCell==null) privCell = new MyLocalSplitter() ; return privCell; }

    final static String NUMERO = "Numero";
    
    static class NBTag implements Tag {

    	final int v;
    	
    	public NBTag(int v) { this.v = v; }
		@Override
		public String getName() { return NUMERO; }

		@Override
		public byte[] getValue() throws AttributeValueException { 
			try {return String.valueOf(v).getBytes("US-ASCII");}
			catch (Exception e) { throw new AttributeValueException(); }
		}
		public int get() { return v; }
		@Override
		public String toString() { return "" + v; }
    }

    private void annotate(Body body) {
    	Chain<Unit> units = body.getUnits();
    	int count = 0;
    	for(Unit u: units) {
    		List <ValueBox> vdefs = u.getDefBoxes();
    		List <ValueBox> vuses = u.getUseBoxes();
    		System.err.print (u + " : ");
    		for (ValueBox vb: vdefs) {
    			System.err.print(vb + "(" + count + ")"); 
    			vb.addTag(new NBTag(count));
    			count ++;
    		}
    		System.err.print(" / ");
    		for (ValueBox vb: vuses) {
    			System.err.print(vb + "(" + count + ")"); 
    			vb.addTag(new NBTag(count));
    			count ++;
    		}
    		System.err.println();
    	}
    	ReducedExceptionalUnitGraph graph = new ReducedExceptionalUnitGraph(body);

        LocalDefs localDefs;
        SimpleLiveLocals lives = new SimpleLiveLocals(graph);
        localDefs = new SmartLocalDefs(graph, lives);

        LocalUses localUses = new SimpleLocalUses(graph, localDefs);
    	
    	for (Unit u: units) {
    		List <ValueBox> vdefs = u.getDefBoxes();
    		List <ValueBox> vuses = u.getUseBoxes();
    		System.err.println (u + " : ");
    		for (ValueBox vb: vdefs) {
    			@SuppressWarnings("unchecked")
    			List <UnitValueBoxPair> luses = localUses.getUsesOf(u);
    			System.err.print("   USE OF " + v(vb) + " : ");
    			for(UnitValueBoxPair p: luses) {
    				System.err.print(v(p.valueBox) + " ");
    			}
    			System.err.println();
    		}
    		for (ValueBox vb: vuses) {
    			if (! (vb.getValue() instanceof Local)) continue;
    			System.err.print("   DEFS OF " + vb + "(" + count + "): ");
    			List <Unit> lu = localDefs.getDefsOfAt((Local) vb.getValue(), u);
    			for(Unit uu : lu) {
    				for (ValueBox vbu: uu.getDefBoxes()) System.err.print(v(vbu) + " ");
    			}
    			System.err.println();
    		}
    		
    	}
    }
    
    private String v(ValueBox vb) {	return vb.getTag(NUMERO).toString(); }
    
	@Override
    protected void internalTransform(Body body, String phaseName, @SuppressWarnings("rawtypes") Map options)
    {
    	if (D2JLog.splitter) annotate(body);
        Chain<Unit> units = body.getUnits();
        List<List<ValueBox>> webs = new ArrayList<List<ValueBox>>();

        if(Options.v().verbose())
            G.v().out.println("[" + body.getMethod().getName() + "] Splitting locals...");

        if(Options.v().time())
                Timers.v().splitPhase1Timer.start();

        // Go through the definitions, building the webs
        {
            ReducedExceptionalUnitGraph graph = new ReducedExceptionalUnitGraph(body);

            LocalDefs localDefs;
            SimpleLiveLocals lives = new SimpleLiveLocals(graph);
            localDefs = new SmartLocalDefs(graph, lives);

            LocalUses localUses = new SimpleLocalUses(graph, localDefs);
            
            if(Options.v().time())
                Timers.v().splitPhase1Timer.end();
    
            if(Options.v().time())
                Timers.v().splitPhase2Timer.start();

            Set<ValueBox> markedBoxes = new HashSet<ValueBox>();
            Map<ValueBox, Unit> boxToUnit = new HashMap<ValueBox, Unit>(units.size() * 2 + 1, 0.7f);
            
            Iterator<Unit> codeIt = units.iterator();

            while(codeIt.hasNext())
            {
                Unit s = (Unit) codeIt.next();

                if (s.getDefBoxes().size() > 1)
                    throw new RuntimeException("stmt with more than 1 defbox!");

                if (s.getDefBoxes().size() < 1)
                    continue;

                ValueBox loBox = (ValueBox)s.getDefBoxes().get(0);
                Value lo = loBox.getValue();

                if(lo instanceof Local && !markedBoxes.contains(loBox))
                {
                    LinkedList<Unit> defsToVisit = new LinkedList<Unit>();
                    LinkedList<ValueBox> boxesToVisit = new LinkedList<ValueBox>();

                    List <ValueBox> web = new ArrayList<ValueBox>();
                    webs.add(web);
                                        
                    defsToVisit.add(s);
                    markedBoxes.add(loBox);
                    // System.err.println("NEW WEB");       
                    while(!boxesToVisit.isEmpty() || !defsToVisit.isEmpty())
                    {
                        if(!defsToVisit.isEmpty())
                        {
                            Unit d = defsToVisit.removeFirst();
                            
                            web.add(d.getDefBoxes().get(0));
                            
                            // Add all the uses of this definition to the queue
                            {
                            	@SuppressWarnings("unchecked")
                                List<UnitValueBoxPair> uses = localUses.getUsesOf(d);
                                Iterator<UnitValueBoxPair> useIt = uses.iterator();
                                // System.err.println("Solving def " + d + " / " + d.getDefBoxes().get(0) + " / " + uses);
                                while(useIt.hasNext())
                                {
                                    UnitValueBoxPair use = (UnitValueBoxPair) useIt.next();
    
                                    if(!markedBoxes.contains(use.valueBox))
                                    {
                                        markedBoxes.add(use.valueBox);
                                        boxesToVisit.addLast(use.valueBox);
                                        boxToUnit.put(use.valueBox, use.unit);
                                    }
                                }
                            }
                        }
                        else {
                            ValueBox box = boxesToVisit.removeFirst();

                            web.add(box);

                            // Add all the definitions of this use to the queue.
                            {               
                                List<Unit> defs = localDefs.getDefsOfAt((Local) box.getValue(),
                                    boxToUnit.get(box));
                                
                                // System.err.println("Solving use " + v(box) + " = " + defs);
            
                                Iterator<Unit> defIt = defs.iterator();
    
                                while(defIt.hasNext())
                                {
                                    Unit u = defIt.next();

                                    Iterator<ValueBox> defBoxesIter = u.getDefBoxes().iterator();
                                    ValueBox b;

                                    for (; defBoxesIter.hasNext(); )
                                    {
                                        b = (ValueBox)defBoxesIter.next();
                                        if(!markedBoxes.contains(b))
                                        {
                                            markedBoxes.add(b);
                                            defsToVisit.addLast(u);
                                        }
                                    }    
                                }
                            }
                        }
                    }
                }
            }
        }

        // Assign locals appropriately.
        {
            Map<Local, Integer> localToUseCount = new HashMap<Local, Integer>(body.getLocalCount() * 2 + 1, 0.7f);
            Iterator<List<ValueBox>> webIt = webs.iterator();

            while(webIt.hasNext())
            {
                List<ValueBox> web = webIt.next();

                ValueBox rep = (ValueBox) web.get(0);
                Local desiredLocal = (Local) rep.getValue();

                if(!localToUseCount.containsKey(desiredLocal))
                {
                    // claim this local for this set

                    localToUseCount.put(desiredLocal, 1);
                }
                // else
                {
                    // generate a new local

                    int useCount = localToUseCount.get(desiredLocal).intValue() + 1;
                    localToUseCount.put(desiredLocal, useCount);
        
                    Local local = (Local) desiredLocal.clone();
                    local.setName(desiredLocal.getName() + "#" + useCount);
                    
                    body.getLocals().add(local);

                    // Change all boxes to point to this new local
                    {
                        Iterator <ValueBox> j = web.iterator();

                        while(j.hasNext())
                        {
                            ValueBox box = (ValueBox) j.next();

                            box.setValue(local);
                        }
                    }
                }
            }
        }
        
        if(Options.v().time())
            Timers.v().splitPhase2Timer.end();

    }   
}
