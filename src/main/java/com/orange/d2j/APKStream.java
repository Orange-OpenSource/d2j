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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;


/**
 * This class provides an entry point for APK read directly as input stream. It memoize the classes and manifest so that they can be
 * accessed later.
 * 
 * @author piac6784
 *
 */
public class APKStream extends APKFile {
	
	/** The code array. */
	private byte [] codeArray;
	
	/** The manifest array. */
	private byte [] manifestArray;
	
	/** The name. */
	final private String name;
	
	/** The Constant SIZE. */
	private final static int SIZE = 16384;
	
	/**
	 * Instantiates a new aPK stream.
	 *
	 * @param name the name
	 * @param stream the stream
	 */
	public APKStream(String name, InputStream stream) {
		this.name = name;
		ZipInputStream zipStream = new ZipInputStream(stream);
		ZipEntry entry;
		try {
			while((entry = zipStream.getNextEntry()) != null) {
				if(entry.getName().equals(CODE)) {
					codeArray = readCurrent(zipStream, entry);
				} else if (entry.getName().equals(MANIFEST)) {
					manifestArray = readCurrent(zipStream, entry);
					
				}
			}
		} catch (ZipException e) {

		} catch (IOException e) {

		}
	}

	/**
	 * Read a given entry and create the corresponding byte array.
	 *
	 * @param stream the inputstream at the correct point
	 * @param entry the entry to extract.
	 * @return A newly created byte array
	 * @throws IOException If anything goes wrong
	 */
	private byte[] readCurrent(ZipInputStream stream, ZipEntry entry) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte [] aux = new byte[SIZE];
		int l;
		while ((l = stream.read(aux)) > 0) {
			out.write(aux,0,l);
		}
		byte [] text = out.toByteArray();
		return text;
	}
	
	/* (non-Javadoc)
	 * @see com.francetelecom.rd.d2j.APKFile#checkVersion()
	 */
	@Override
	public boolean checkVersion() {
		if (manifestArray == null) {
			D2JLog.warning("No manifest in stream.");
			return false;
		}
		if (codeArray == null) {
			D2JLog.warning("No code in stream.");
			return false;
		}
		byte [] magic = Arrays.copyOf(codeArray,MAGIC_TEMPLATE.length);
		return Arrays.equals(magic, MAGIC_TEMPLATE);
	}
	
	/* (non-Javadoc)
	 * @see com.francetelecom.rd.d2j.APKFile#codeStream()
	 */
	@Override
	public InputStream codeStream() throws IOException {
		return new ByteArrayInputStream(codeArray);
	}
	
	/* (non-Javadoc)
	 * @see com.francetelecom.rd.d2j.APKFile#manifestStream()
	 */
	@Override
	public InputStream manifestStream() throws IOException {
		return new ByteArrayInputStream(manifestArray);
	}

	/* (non-Javadoc)
	 * @see com.francetelecom.rd.d2j.APKFile#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
}
