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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import soot.ArrayType;
import soot.DoubleType;
import soot.FloatType;
import soot.Local;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Type;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;

/**
 * This class handles the typing of a method body.
 * @author piac6784
 *
 */
public class DvkTyper {
	
	final static int IS_TYPE = 1;
	final static int IS_SUB = 2;
	final static int IS_SUPER = 3;
	
	final static int IS_UKW = 0;
	final static int IS_INT = 1;
	final static int IS_LONG = 2;
	final static int IS_FLOAT = 3;
	final static int IS_DOUBLE = 4;
	final static int IS_OBJECT = 5;

	final static int ARRAY_MASK = 0xfffffff8;
	final static int TYPE_MASK = 0x7;
	final static int ARRAY_OFFSET = 3;
	
	private Map <ValueBox, Integer> boxTyping = new HashMap<ValueBox,Integer>();
	private Map <Local, Integer> localTyping = new HashMap<Local,Integer>();
	private List <DeferedTypeConstraint> deferedConstraints = new ArrayList <DeferedTypeConstraint>();
	private Set <ValueBox> boxDelayedTyping = new HashSet<ValueBox>();
	
	/**
	 * Transform a Soot type into a type category. 
	 * This is only used to type constants.
	 * @param type
	 * @return
	 */
	public int kind(Type type) {
		int kind = 0;
		if (type instanceof ArrayType) {
			// Assumes that the base type cannot be another array type.
			ArrayType at = (ArrayType) type;
			return at.numDimensions << ARRAY_OFFSET | kind(at.baseType);
		} else if (type instanceof PrimType) {
			if (type instanceof FloatType) kind = IS_FLOAT;
			else if (type instanceof DoubleType) kind = IS_DOUBLE;
			else if (type instanceof LongType) kind = IS_LONG;
			else kind=IS_INT;
		} else if (type instanceof RefType) { kind = IS_OBJECT;	}
		return kind;
	}
	
	/**
	 * Inform the type checker of a hard constraint on a variable (We know what it is).
	 * @param v
	 * @param type
	 */
	public void setType(ValueBox v, Type type) {
		
		if (D2JLog.typing) D2JLog.info("Setting hard constraint " + v + " to " + type);
		if (RefType.v("java.lang.Object").equals(type)) boxDelayedTyping.add(v);
		else {
			int kind = kind(type);
			if(kind != 0) boxTyping.put(v, kind);
		}
	}
	
	/**
	 * Constrain to a ref type. We just know it is not a primitive type but it may be an array.
	 * @param v
	 */
	public void setObjectType(ValueBox v) {
		boxDelayedTyping.add(v);
	}
	
	private void setType(Local l, int kind, DeferedTypeConstraint tc) {
		if (D2JLog.typing) D2JLog.info("Type for local " + l + " set to " + kind + " (" + tc + ")");
		localTyping.put(l, kind);
	}
	
	private int getLocalType(Local l) {
		Integer i = localTyping.get(l);
		return (i==null) ? 0 : i;
	}
	
	/**
	 * This represents a type constraints between two values contained in a box.
	 * It means they are in the same category : not less, not more, but it is still
	 * oriented because of array types.
	 * @author piac6784
	 *
	 */
	static class DeferedTypeConstraint {
		/**
		 * First value 
		 */
		final ValueBox box1;
		/**
		 * Second value
		 */
		final ValueBox box2;
		
		/**
		 * @param box
		 * @param typeWitness
		 */
		DeferedTypeConstraint(ValueBox box1, ValueBox box2) {
			this.box1 = box1;
			this.box2 = box2;
		}
		
		@Override
		public String toString() {
			return box2.getValue() + " <: " + box1.getValue();
		}
	}

	void setConstraint(ValueBox box1, ValueBox box2) {
		deferedConstraints.add(new DeferedTypeConstraint(box1,box2));
	}

	void assignType() {
		for(Entry<ValueBox, Integer> entry : boxTyping.entrySet()) {
            Value v = entry.getKey().getValue();
            if (!(v instanceof Local)) continue;
            Local l = (Local) v;
			Integer type = entry.getValue();
			localTyping.put(l, type);
		}
		resolveConstraints();
		boolean modified = false;
		for(ValueBox vb : boxDelayedTyping) {
            Value v = vb.getValue();
            if (!(v instanceof Local)) continue;
            Local l = (Local) v;
			if (localTyping.get(l) == null ) {
				localTyping.put(l,IS_OBJECT);
				modified = true;
			}
		}
		if (modified) resolveConstraints();
	}

	void resolveConstraints() {
		int size = 0;
		int old_size = Integer.MAX_VALUE;
		if(D2JLog.typing) D2JLog.info("CONSTRAINTS " + deferedConstraints.toString());
		while ((size = deferedConstraints.size()) != 0 && size < old_size) {
			old_size = size;
			
			Iterator <DeferedTypeConstraint> it = deferedConstraints.iterator();
			while (it.hasNext()) {
				DeferedTypeConstraint tc = it.next();
				Value v1 = tc.box1.getValue();
				Value v2 = tc.box2.getValue();
				if (v2 instanceof Constant && v1 instanceof Local) {
					int type = getLocalType((Local) v1);
					switch (type) {
					case IS_UKW:
						break;
					case IS_INT:
						if (!(v2 instanceof IntConstant)) D2JLog.warning("Expected int");
						it.remove();
						break;
					case IS_FLOAT:
						if (v2 instanceof IntConstant) {
							tc.box2.setValue(FloatConstant.v(Float.intBitsToFloat(((IntConstant) v2).value)));
							it.remove();
						} else  D2JLog.warning("Expected int (for float)");
						break;
					case IS_DOUBLE:
						if ((v2 instanceof LongConstant) || (v2 instanceof IntConstant)) {
							long vv =
								(v2 instanceof LongConstant) ? ((LongConstant) v2).value : ((IntConstant) v2).value; 
							tc.box2.setValue(DoubleConstant.v(Double.longBitsToDouble(vv)));
							it.remove();
						} else D2JLog.warning("Expected long (for double)");
						break;
					case IS_LONG:
						if (v2 instanceof IntConstant) {
							long vv = ((IntConstant) v2).value;
							tc.box2.setValue(LongConstant.v(vv));
							it.remove();
						} else if (!(v2 instanceof LongConstant)) D2JLog.warning("Expected long");
						break;
					// Either an object or an array type
					default:
						if (v2 instanceof IntConstant && ((IntConstant) v2).value == 0) {
							tc.box2.setValue(NullConstant.v());
						} else if (!(v2 instanceof NullConstant)) {
							D2JLog.warning("Expected null " + v1 + "=" + v2);
						}
					}
				} else if (v2 instanceof Local && v1 instanceof Local) {
					Local l1 = (Local) v1;
					Local l2 = (Local) v2;
					int type1 = getLocalType(l1);
					int type2 = getLocalType(l2);
					if (type1 != IS_UKW) {
						setType(l2,type1, tc);
						it.remove();
					} else if (type2 != IS_UKW){
						setType(l1,type2, tc);
						it.remove();
					}
				} else if (v1 instanceof ArrayRef && ((ArrayRef) v1).getBase() instanceof Local && v2 instanceof Local) {
					Local l1 = (Local) ((ArrayRef) v1).getBase();
					Local l2 = (Local) v2;
					int type1 = getLocalType(l1);
					if (type1 != IS_UKW) {
						int base_type = type1 & TYPE_MASK;
						int array_size = (type1 & ARRAY_MASK) >> ARRAY_OFFSET;
						if (array_size == 0) {
							D2JLog.warning("Array size is 0 for " + v1);
						}
						int type2 = base_type | (array_size - 1 ) << ARRAY_OFFSET;
						setType(l2, type2, tc);
						it.remove();
					}
				} else D2JLog.warning("bad array constraint" + v1 + " - " + v2);
			}
		}
	}
	
	/**
	 * Debugging function
	 */
	public void dumpConstraints() {
		for(Entry<Local,Integer> e: this.localTyping.entrySet()) {
			System.out.println(e.getKey() + " : " + e.getValue());
		}
	}
}
