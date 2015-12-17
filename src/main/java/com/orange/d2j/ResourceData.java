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


/**
 * Codes data found in compiled XML resource files.
 * @author Pierre Cregut
 *
 */
public class ResourceData {
	
	/**
	 * Coding of colors.
	 * @author Pierre Cregut
	 *
	 */
	public static class Color {
		
		/** red component (0-255). */
		private final int red;
		
		/** green component (0-255). */
		private final int green;
		
		/** blue component (0-255). */
		private final int blue;
		
		/** alpha component (0-255). */
		private final int alpha;

		/**
		 * Constructor (rgb).
		 *
		 * @param r the r
		 * @param g the g
		 * @param b the b
		 */
		public Color(int r, int g, int b) {
			red = r; green = g; blue = b; alpha = 255;
		}
		
		/**
		 * Constructor (rgba).
		 *
		 * @param r the r
		 * @param g the g
		 * @param b the b
		 * @param a the a
		 */
		public Color(int r, int g, int b, int a) {
			red = r; green = g; blue = b; alpha = a;
		}
		
        /**
         * Gets the red component.
         *
         * @return the red byte
         */
        public int getRed() {
            return red;
        }
        
        /**
         * Gets the green component.
         *
         * @return the green byte
         */
        public int getGreen() {
            return green;
        }
        
        /**
         * Gets the blue component.
         *
         * @return the blue byte
         */
        public int getBlue() {
            return blue;
        }
        
        /**
         * Gets the alpha.
         *
         * @return the alpha byte
         */
        public int getAlpha() {
            return alpha;
        }
	}
    
    /** A null entry. */
    final public static int TYPE_NULL = 0x00;
    /** The 'data' holds a ResTable_ref, a reference to another resource
    	table entry. */
    final public static int TYPE_REFERENCE = 0x01;
    /** The 'data' holds an attribute resource identifier. */
    final public static int TYPE_ATTRIBUTE = 0x02;
    /** The 'data' holds an index into the containing resource table's 
     * global value string pool. */
    final public static int TYPE_STRING = 0x03;
    /** The 'data' holds a single-precision floating point number. */
    final public static int TYPE_FLOAT = 0x04;
    /** The 'data' holds a complex number encoding a dimension value, such as "100in". */
    final public static int TYPE_DIMENSION = 0x05;
    /** The 'data' holds a complex number encoding a fraction of a container. */
    final public static int TYPE_FRACTION = 0x06;


    /** The 'data' is a raw integer value of the form n..n. */
    final public static int TYPE_INT_DEC = 0x10;
    /** The 'data' is a raw integer value of the form 0xn..n. */
    final public static int TYPE_INT_HEX = 0x11;
    /** The 'data' is either 0 or 1, for input "false" or "true" respectively. */
    final public static int TYPE_INT_BOOLEAN = 0x12;
    /** The 'data' is a raw integer value of the form #aarrggbb. */
    final public static int TYPE_INT_COLOR_ARGB8 = 0x1c;
    /** The 'data' is a raw integer value of the form #rrggbb. */
    final public static int TYPE_INT_COLOR_RGB8 = 0x1d;
    /** The 'data' is a raw integer value of the form #argb. */
    final public static int TYPE_INT_COLOR_ARGB4 = 0x1e;
    /** The 'data' is a raw integer value of the form #rgb. */
    final public static int TYPE_INT_COLOR_RGB4 = 0x1f;

    /**
     * What is coded in this value.
     */
    private final Object data;
    /**
     * Its type code in Android format.
     */
    private final int type;
    
    /**
     * The Class Reference.
     *
     * @author Pierre Cregut
     * A reference to a resource entry
     */
    public static class Reference {
    	/**
    	 * Identifier of the referenced object.
    	 */
    	public final int id;
    	
	    /**
	     * Instantiates a new reference.
	     *
	     * @param id the id
	     */
	    Reference(int id) {this.id = id;}
    	
	    /* (non-Javadoc)
	     * @see java.lang.Object#toString()
	     */
	    @Override
		public String toString() {
    		return "@" + Integer.toHexString(id);
    	}
    }

    /**
     * The Class Attribute.
     *
     * @author Pierre Cregut
     * An attribute in the current them style
     */
    public static class Attribute {
        /**
         * Identifier of the referenced object.
         */
        public final int id;
        
        /**
         * Instantiates a new attribute.
         *
         * @param id the id
         */
        Attribute(int id) {this.id = id;}
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "@" + Integer.toHexString(id);
        }
    }
    /**
     * Represent dimensions data value.
     * @author Pierre Cregut
     *
     */
    public static class Dimension {
    	
    	/**
    	 * Mantissa.
    	 */
    	public final int mantissa;
    	/**
    	 * Radix code of the value.
    	 */
    	public final int radix;
    	
	    /** Code of the unit in which the dimension is expressed. */
    	public final int unit;
    	
	    /**
	     * Instantiates a new dimension.
	     *
	     * @param m the m
	     * @param radix the radix
	     * @param unit the unit
	     */
	    Dimension(int m, int radix, int unit) {
    		this.mantissa = m;
    		this.radix = radix;
    		this.unit = unit;
    	}
       	
	       /* (non-Javadoc)
	        * @see java.lang.Object#toString()
	        */
	       @Override
		public String toString() {
       		String u;
       		switch(unit) {
       		case 0:
       			u = "Pixels"; break;
       		case 1:
       			u = "DIP"; break;
       		case 2:
       			u = "scaled DIP"; break;
       		case 3:
       			u = "pt"; break;
       		case 4:
       			u = "inch"; break;
       		case 5:
       			u = "mm"; break;
       		default:	
       			u="ukw";
       		}
    		return ((double) mantissa / (1 << (radix * 8))) + " " + u;
    	}
    }

    /**
     * Represent fraction data values.
     *
     * @author Pierre Cregut
     */
    public static class Fraction {
    	/**
    	 * Mantissa of the fraction.
    	 */
    	public final int mantissa;
    	
	    /** Radix of the fraction. */
    	public final int radix;
    	/**
    	 * Is the fraction an absolute (top level one) or relative to the parent.
    	 */
    	public final boolean in_parent;
    	
	    /**
	     * Instantiates a new fraction.
	     *
	     * @param m the m
	     * @param radix the radix
	     * @param in_parent the in_parent
	     */
	    Fraction(int m, int radix, boolean in_parent) {
    		this.mantissa = m;
    		this.radix = radix;
    		this.in_parent = in_parent;
    	}
    	
	    /* (non-Javadoc)
	     * @see java.lang.Object#toString()
	     */
	    @Override
		public String toString() {
    		return (in_parent ? "Rel" : "Abs")  + ((double) mantissa / (1 << (radix * 8)));
    	}
    }

    /**
     * Constructor. It is just a pair of an almost arbitrary object and its type.
     *
     * @param data the data
     * @param type the type
     */
    public ResourceData(Object data, int type) {
    	this.type = type;
    	this.data = data;
    }
    
	/**
	 * Parses a data value from a stream. It needs a pool of strings to interpret references
	 * to strings.
	 *
	 * @param reader the reader
	 * @param strings the strings
	 * @return the resource data
	 */
	public static ResourceData parseData(DalvikValueReader reader, String [] strings) {
		int pos = reader.getPos();
    	int sz = reader.ushort();
    	if (reader.ubyte() != 0) throw new RuntimeException("0 expected");
    	int kind = reader.ubyte();
    	Object object;
    	switch(kind) {
        case TYPE_NULL:
        	object = null;
        	break;
        case TYPE_REFERENCE:
        	object = new Reference((int) reader.uint());
        	break;
        case TYPE_ATTRIBUTE:
        	object = new Attribute((int) reader.uint());
        	break;      
        case TYPE_STRING:
        	object = strings[(int) reader.uint()];
        	break;
        case TYPE_FLOAT:
        	object = Float.intBitsToFloat(reader.sint());
        	break;
        case TYPE_DIMENSION:
        	int v = (int) reader.uint();
        	object = new Dimension(v >> 8, (v >> 4) & 0x3, v & 0xf);
        	break;
        case TYPE_FRACTION:
        	v = (int) reader.uint();
        	object = new Fraction(v >> 8, (v >> 4) & 0x3, (v & 1) == 1 );
        	break;
        case TYPE_INT_DEC:
        case TYPE_INT_HEX: // We loose whether it was coded as a decimal or an hex number
        	object = Integer.valueOf((int) reader.uint());
        	break;
        case TYPE_INT_BOOLEAN:   	
         	object = Boolean.valueOf(reader.uint() != 0);
        	break;
        case TYPE_INT_COLOR_ARGB8:
        	long color = reader.uint();
        	object = new Color((int) (color >> 16 & 0xff), (int) (color >> 8 & 0xff),(int) (color & 0xff), (int) (color >> 24 & 0xff));
        	break;
        case TYPE_INT_COLOR_RGB8:
        	color = reader.uint();
        	object = new Color((int) color >> 16 & 0xff, (int) color >> 8 & 0xff, (int) color & 0xff);
        	break;
        case TYPE_INT_COLOR_ARGB4:
        	int col2 = (int) reader.uint();
        	object = new Color(ext48(col2 >> 8 & 0xf), ext48(col2 >> 4 & 0xf), ext48(col2 & 0xf), ext48(col2 >> 12 & 0xf));
        	break;
        case TYPE_INT_COLOR_RGB4:
        	col2 = (int) reader.uint();
        	object = new Color(ext48(col2 >> 8 & 0xf), ext48(col2 >> 4 & 0xf), ext48(col2 & 0xf));
        	break;
        default:
        	object = null;
        	D2JLog.warning("unknown data type." + kind);
    	}
    	reader.seek(pos + sz);
    	return new ResourceData(object, kind);
	}
	
	/**
	 * Ext48.
	 *
	 * @param x the x
	 * @return the int
	 */
	private static int ext48(int x) { return x | (x << 4); }

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return (getData() == null) ? "" : getData().toString();
	}

    /**
     * Gets the data.
     *
     * @return the data
     */
    public Object getData() {
        return data;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public int getType() {
        return type;
    }
}
