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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.orange.d2j.DalvikValueReader;

/**
 * @author Pierre Cregut
 * Test the reading of UTF8 modified strings. 
 */

@RunWith(Parameterized.class)
public class UTF8Test {
	static Object [][] data = new Object [][] {
			{ "S", new byte [] { 0x1F, 0x0}, "\u001F" },
			{ "D", new byte [] { (byte) 0xC2, (byte) 0x80, 0x0 }, "\u0080" },
			{ "DD", new byte [] { (byte) 0xC2, (byte) 0x9F, (byte) 0xC3, (byte) 0xA9, 0x0 }, "\u009F\u00E9" },
			{ "T", new byte [] {(byte) 0xE2, (byte) 0x82, (byte) 0xAC, 0 }, "\u20ac" },
			{ "TT", new byte [] {(byte)0xE0, (byte) 0xA0, (byte) 0x80, (byte) 0xEF, (byte) 0xBF, (byte) 0xBF, 0}, "\u0800\uFFFF" }
	};
	
	/**
	 * Initializes tests params.
	 * @return
	 */
	@Parameters(name="{index}:{0}")
	public static List <Object []> init() {	return Arrays.asList(data);	}

	private byte[] coding;
	private String value;

	/**
	 * Constructor with the string to test.
	 * @param name name to display
	 * @param coding coding of the string
	 * @param value string to check
	 */
	public UTF8Test(String name, byte [] coding, String value) {
		this.coding = coding;
		this.value = value;
	}
	
	/**
	 * The test itself.
	 */
	@Test
	public void utf8() {
		DalvikValueReader r = new DalvikValueReader(coding);
		String decoded = r.utf8String();
		Assert.assertTrue(decoded + " != " + value, value.equals(decoded));
	}
}
