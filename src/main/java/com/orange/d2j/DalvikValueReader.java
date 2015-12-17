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
import java.util.Arrays;

/**
 * Reads the different kinds of primitive Dalvik values from an encapsulated stream
 * @author Pierre Cregut
 *
 */
public class DalvikValueReader {
	private byte [] contents;
	private int pos;

	/**
	 * Constructor encapsulating an array of bytes.
	 * @param stream
	 */
	public DalvikValueReader(byte [] contents) {
		this.contents = contents;
	}

	
	/**
	 * Constructor encapsulating an input stream. We need to find the size of the file
	 * This is given by the offset. We first read up to the size, and then fill the byte
	 * array. The size in the byte array is NOT correct.
	 * @param stream the input stream containing the resource/dex
	 * @param size_offset offset from the begining to find the total file size
	 * @throws IOException
	 */
	public DalvikValueReader(InputStream stream, int size_offset) throws IOException {
   		byte [] header = new byte[size_offset];
		if (stream.read(header) != size_offset) 
			throw new IOException("Cannot skip Resource header");
		int file_size = sint(stream);
		// stream.getChannel().position(0);
		byte [] file = new byte [file_size];
		int to_skip = size_offset + 4;
		while(file_size - to_skip != 0) {
			int read =stream.read(file, to_skip, file_size - to_skip); 
			if (read == 0) 
				throw new IOException("Truncated Resource file " + read + "/" + file_size + "/" + to_skip);
			to_skip += read;
		}
		System.arraycopy(header, 0, file, 0, size_offset);
		this.contents = file;
	}

	/**
	 * Reads an integer directly from an input stream. Usually used
	 * while builder the value reader.
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	public static int sint(InputStream stream) throws IOException {
		byte [] contents = new byte [4];
		if (stream.read(contents) != 4) throw new RuntimeException("Cannot read integer");
		return ((contents [0] & 0xFF) | ((contents[1] & 0xFF) << 8) | ((contents[2] & 0xFF) << 16) | ((contents[3] & 0xFF) << 24));
	}

	/**
	 * Reads next signed byte value.
	 * @return value read from stream
	 * @throws IOException
	 */
	public byte sbyte()  {
		return contents[pos++];
	}

	/**
	 * Reads next unsigned byte value.
	 * @return value read from stream
	 */
	public short ubyte()  {
		return (short) (contents[pos++] & 0xff);
	}

	/**
	 * Reads next signed short value.
	 * @return value read from stream
	 */
	public short sshort() {
		short v = (short) ((contents [pos] & 0xff)| ((contents[pos + 1] & 0xff) << 8));
		pos += 2;
		return v;
	}

	/**
	 * Reads next unsigned short value.
	 * @return value read from stream
	 */
	public int ushort()  {
		int v = ((contents [pos] & 0xff) | ((contents[pos + 1] & 0xff) << 8));
		pos += 2;
		return v;
	}

	/**
	 * Reads next signed integer value.
	 * @return value read from stream
	 */
	public int sint() {
		int v = ((contents [pos] & 0xff)| ((contents[pos + 1] & 0xff) << 8) | ((contents[pos + 2] & 0xff) << 16) | ((contents[pos + 3] & 0xff) << 24));
		pos += 4;
		return v;
	}

	/**
	 * Reads next unsigned integer value.
	 * @return value read from stream
	 */
	public long uint() {
		long v = (long) ((contents [pos] & 0xff) | ((contents[pos + 1] & 0xff) << 8) | ((contents[pos + 2] & 0xff) << 16)) | ((long) (contents[pos + 3] & 0xff) << 24);
		pos += 4;
		return v;
	}

	/**
	 * Reads next signed integer value coded in leb128 format
	 * @return value read from stream
	 */
	public int sleb128() {
		int r, v;
		v = contents[pos++] & 0xff;
		r = v & 0x7f;
		if (v >= 0x80) {
			v = contents[pos++] & 0xff;
			r |= (v & 0x7f) << 7;
			if (v >= 0x80) {
				v = contents[pos++] & 0xff;
				r |= (v & 0x7f) << 14;
				if (v >= 0x80) {
					v = contents[pos++] & 0xff;
					r |= (v & 0x7f) << 21;
					if (v >= 0x80) {
						v = contents[pos++] & 0xff;
						r |= (v & 0x7f) << 28;
						if (v >= 0x80) {
							throw new RuntimeException("Bad sleb128");
						}
					} else if ((v & 0x40) != 0) r |= 0xf0000000;
				} else if ((v & 0x40) != 0) r |= 0xffe00000;
			} else if ((v & 0x40) != 0) r |= 0xffffc000;
		} else if ((v & 0x40) != 0) r |= 0xffffff80;
		return r;
	}

	/**
	 * Reads next unsigned integer value coded in leb128 format
	 * @return value read from stream
	 */
	public long uleb128(){
		long r;
		int v;
		v = contents[pos++] & 0xff;
		r = v & 0x7f;
		if (v >= 0x80) {
			v = contents[pos++] & 0xff;
			r |= (v & 0x7f) << 7;
			if (v >= 0x80) {
				v = contents[pos++] & 0xff;
				r |= (v & 0x7f) << 14;
				if (v >= 0x80) {
					v = contents[pos++] & 0xff;
					r |= (v & 0x7f) << 21;
					if (v >= 0x80) {
						v = contents[pos++] & 0xff;
						r |= (v & 0x7f) << 28;
						if (v >= 0x80) {
							throw new RuntimeException("Bad sleb128");
						}
					}
				}
			}
		}
		return r;

	}

	/**
	 * Reads next unsigned integer value coded in leb128 format but with 16 bits chars.
	 * @return value read from stream
	 */
	public long uleb128_16(){
		long r;
		int v;
		v = ushort();
		r = v & 0x7fff;
		if (v > 0x8000) {
			v = ushort();
			r |= (v & 0x7fff) << 15;
		}
		return r;
	}

	/**
	 * Reads a long of a given size. Considered as unsigned.
	 * @param l
	 * @return
	 */
	public long sizedLong(int sz) {
		long result = 0;
		for(int i=0; i < sz + 1; i++) {
			short v = ubyte();
			result = result | v << (8*i); 
		}
		return result;
	}
	
	/**
	 * Extends a long read with SizedLong of length sz according to its sign.
	 * @param l
	 * @param sz
	 * @return
	 */
	public static long completeSignSizedLong(long l, int sz) {
		long pattern = 0x80L << (sz * 8);
		if ((pattern & l) != 0) {
			for(int i = sz + 1; i < 8; i++) {
				l |= 0xff << (i * 8);
			}
		} 
		return l;
	}
	
	/**
	 * Reads a given number of bytes.
	 * @param b
	 * @return
	 */
	public void bytes(byte [] b) { 
		for(int i=0; i < b.length; i++) b[i] = contents[pos+i];
		pos += b.length;
	}

	/**
	 * Reads a null terminated UTF8 string as handled by Dalvik (limited to unicode)
	 * @return
	 */
	public String utf8String() {
		StringBuilder buf = new StringBuilder();
		int c;
		int v;
		while( (c = (contents[pos++] & 0xff)) != 0) {
			if ((c & 0x80) == 0x80) {
				if ((c & 0xe0) == 0xc0) {
					c &= 0x1f;
					v = contents[pos++] & 0x3f;
					c =  c << 6 | v;
				} else if ((c & 0xf0) == 0xe0) {
					v = contents[pos++] & 0x3f;
					c = c << 6 | v;
					v = contents[pos++] & 0x3f;
					c = c << 6 | v;
				} else {
					D2JLog.warning("Bad (point 4) UTF 8 " + Integer.toBinaryString(c));
				}
			}
			buf.append((char) c);
		}
		return buf.toString();
	}
	
	/**
	 * Set the position of the pointer in the stream.
	 * @param pos
	 */
	public void seek(int pos) { this.pos = pos; }

	/**
	 * Get the current position of the pointer in the stream. Usually to make a save restore.
	 * @return
	 */
	public int getPos() { return pos; }

	/**
	 * Skip a given number of bytes in the stream
	 * @param offset the number of bytes to skip
	 */
	public void skip(int offset) { this.pos += offset; }
	
	/**
	 * Parse a string coded as 16 bit character
	 * @param strSize
	 * @return
	 */
	public String unicodeString(int strSize) {
		char [] content = new char [strSize];
		for(int i=0; i < strSize; i++) content[i] = (char) ushort();
		int c;
		if ((c = ushort()) != 0) {
			D2JLog.warning("Did not find the ending character\n " + Arrays.toString(content) + " " + c );
		}
		return new String(content);
	}


	/**
	 * Check if there are still data to read in the stream.
	 * @return
	 */
	public boolean hasMore() {
		return pos < contents.length;
	}
	
	// For debug only - not exposed.
	int peek(int i) { return ((int) contents[i]) & 0xff; }
}
