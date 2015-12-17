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

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the basic structure of a DEX file and builds a representation of this file.
 * @author Pierre Cregut
 *
 */

public class BasicDexFile {

	
	/**
	 * Constant representing the absence of an index in a table especially for super class.
	 */
	public final static int NO_INDEX = 0xFFFFFFFF;
	
	/** The reader. */
	protected DalvikValueReader reader;
	
	/** The magic header. */
	private byte [] magic = new byte[8];
	
	/** ADLER 32 checksum of the DEX file. */
	private long adler32;

	/** Signature of the DEX file. */
	public byte [] signature = new byte [20];

	/** Standard or reverse endianess. */
	private boolean endianess;
	
	/** Link size. */
	private int linkSize;
	
	/** The string_ids_size. */
	protected int string_ids_size;
	
	/** The string_ids_off. */
	protected int string_ids_off;
	
	/** The string constant pool. */
	String strings[];

	/** The type_ids_size. */
	protected int type_ids_size;
	
	/** The type_ids_off. */
	protected int type_ids_off;

	/** The proto_ids_size. */
	protected int proto_ids_size;
	
	/** The proto_ids_off. */
	protected int proto_ids_off;

	/** The field_ids_size. */
	protected int field_ids_size;
	
	/** The field_ids_off. */
	protected int field_ids_off;

	/** The method_ids_size. */
	protected int method_ids_size;
	
	/** The method_ids_off. */
	protected int method_ids_off;

	/** The class_defs_size. */
	protected int class_defs_size;
	
	/** The class_defs_off. */
	protected int class_defs_off;
	
	/**
	 * Constructor.
	 */
	public BasicDexFile() {
	}

	/**
	 * Takes an input stream coresponding to a dex file and populate the structure.
	 *
	 * @param is the is
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws RefNotFoundException the ref not found exception
	 */
	public void parse(InputStream is) throws IOException, RefNotFoundException {
		reader = new DalvikValueReader(is, 32);
		readHeader(is);
		readStrings();
	}

	/**
	 * Read header.
	 *
	 * @param stream the stream
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	protected void readHeader(InputStream stream) throws IOException {
		reader.bytes(magic);
		if (magic[4] != 0x30 || magic[5] != 0x33 || magic[6] != 0x35) throw new IOException("Unknown bytecode version");
		adler32 = reader.uint();
		reader.bytes(signature);
		reader.sint();
		int header_size = reader.sint();
		if (header_size != 0x70) throw new IOException("Bad header size: " + Integer.toHexString(header_size));
		endianess = reader.sint() == 0x12345678;
		linkSize = reader.sint();
		/* link_off = */ reader.sint();
		/* map_off = */ reader.sint();

		string_ids_size = reader.sint();
		string_ids_off = reader.sint();

		type_ids_size = reader.sint();
		type_ids_off = reader.sint();

		proto_ids_size = reader.sint();
		proto_ids_off = reader.sint();

		field_ids_size = reader.sint();
		field_ids_off = reader.sint();

		method_ids_size = reader.sint();
		method_ids_off = reader.sint();

		class_defs_size = reader.sint();
		class_defs_off = reader.sint();

		/* data_size = */ reader.sint();
		/* data_off = */ reader.sint();

	}

	/**
	 * Read the string index table.
	 */
	protected void readStrings() {
		reader.seek(string_ids_off);
		int [] offsets = new int [string_ids_size];
		int size;
		String string;
		for(int i=0; i < string_ids_size; i++) {
			offsets[i] = reader.sint();
		}
		strings = new String [string_ids_size];
		for(int i=0; i < string_ids_size; i++) {
			reader.seek(offsets[i]);
			size = (int) reader.uleb128();
			string = reader.utf8String();
			strings[i] = string;
			if (string.length() != size) {
				StringBuilder b = new StringBuilder();
				for(int k=offsets[i]; k < reader.getPos(); k++) b.append(" ").append(Integer.toHexString(reader.peek(k)));
				D2JLog.warning("Mismatched size for strings " + string + " " +
						size + " / " + string.length() + b);
			}
		}
	}

    /**
     * Gets the adler32.
     *
     * @return the adler32
     */
    public long getAdler32() {
        return adler32;
    }

    /**
     * Checks if is endianess.
     *
     * @return true, if is endianess
     */
    public boolean isEndianess() {
        return endianess;
    }


    /**
     * Gets the link size.
     *
     * @return the link size
     */
    public int getLinkSize() {
        return linkSize;
    }

	
}
