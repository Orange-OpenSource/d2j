package com.orange.d2j.tests;

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

import org.junit.Assert;

import org.junit.Test;

import com.orange.d2j.DalvikValueReader;

/**
 * Tests on the decoding of LEB128 values, signed or not.
 * @author Pierre Cregut
 *
 */
public class LEB128ReaderTest {
	final static byte [] a1 = { 0x4, 0x45, 0x0 };
	final static byte [] a2 = { (byte) 0x84, 0x6, (byte) 0xf4, 0x47, 0x0 };
	final static byte [] a3 = { (byte) 0x92, (byte) 0x84, (byte) 0xd9, (byte) 0xc3, 0x3};
	final static byte [] a4 = { (byte) 0x92, (byte) 0x84, (byte) 0xd9, (byte) 0x43};
	
	/** Unsigned simple */
	@Test
	public void uleb128Test1() {
		DalvikValueReader r = new DalvikValueReader(a1);
		int v = (int) r.uleb128();
		Assert.assertTrue(v == 4);
		v =  (int) r.uleb128();
		Assert.assertTrue(v == 0x45);
		Assert.assertTrue(r.getPos() == 2);
	}
	
	/** Unsigned simple */
	@Test
	public void uleb128Test2() {
		DalvikValueReader r = new DalvikValueReader(a2);
		int v = (int) r.uleb128();
		Assert.assertTrue(v == 0x304);
		v =  (int) r.uleb128();
		Assert.assertTrue(v == 0x23f4);
		Assert.assertTrue(r.getPos() == 4);
	}
	
	/** Unsigned very long */
	@Test
	public void uleb128Test3() {
		DalvikValueReader r = new DalvikValueReader(a3);
		int v = (int) r.uleb128();
		Assert.assertTrue(v == 0x38764212);
	}

	/** Unsigned total scope */
	@Test
	public void uleb128Test4() {
		DalvikValueReader r = new DalvikValueReader(a4);
		int v = (int) r.uleb128();
		Assert.assertTrue(v == 0x8764212);
	}

	/** signed version of test1 */
	@Test
	public void sleb128Test1() {
		DalvikValueReader r = new DalvikValueReader(a1);
		int v = (int) r.sleb128();
		Assert.assertTrue(v == 4);
		v =  (int) r.sleb128();
		Assert.assertTrue(v == 0xffffffc5);
		Assert.assertTrue(r.getPos() == 2);
	}
	
	/** signed version of test 2 */
	@Test
	public void sleb128Test2() {
		DalvikValueReader r = new DalvikValueReader(a2);
		int v = (int) r.sleb128();
		Assert.assertTrue(v == 0x304);
		v =  (int) r.sleb128();
		
		Assert.assertTrue(v == 0xffffe3f4);
		Assert.assertTrue(r.getPos() == 4);
	}
	
	/** signed version of test 3. beware sign extension */
	@Test
	public void sleb128Test3() {
		DalvikValueReader r = new DalvikValueReader(a3);
		int v = (int) r.uleb128();
		Assert.assertTrue(v == 0x38764212);
	}

	/** signed version of test 4. */
	@Test
	public void sleb128Test4() {
		DalvikValueReader r = new DalvikValueReader(a4);
		int v = (int) r.sleb128();
		Assert.assertTrue(v == 0xf8764212);
	}

}
