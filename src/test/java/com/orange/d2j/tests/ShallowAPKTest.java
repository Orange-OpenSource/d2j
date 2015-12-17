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

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.orange.d2j.Hierarchy;


/**
 * A parameterized JUnit test that checks how a set of APK are parsed (only at the structure level).
 * @author Pierre Cregut
 *
 */
@RunWith(Parameterized.class)
public class ShallowAPKTest {

    private static final File BASE = new File(new File(new File("src"),"test"),"cases");
	private static final File APK = new File(BASE,"apk");
	   

	/**
	 * Only keeps test ending with .apk.
	 *
	 */
	private static class APKFilter implements FileFilter {
		@Override
		public boolean accept(File arg) { return arg.getName().endsWith(".apk"); }

	}
	
	/**
	 * Builds the list of all dex test files (in folder dex).
	 * @return
	 */
	@Parameters(name="{index}:{0}")
	public static List <Object []> filenames() {
		List <Object []> result = new ArrayList <Object []> ();

		File[] allFiles = APK.listFiles(new APKFilter());
		if (allFiles == null)
			return result;
		Arrays.sort(allFiles);
		for (int i = 0; i < allFiles.length; i++)
			result.add(new Object[] { allFiles[i] });
		
		return result;
	}

	private File filename;
	
	/**
	 * This is a parameterized constructor. 
	 * @param filename
	 */
	public ShallowAPKTest(File filename) {
		this.filename = filename;
	}

	/**
	 * Check the compilation of a Dex File.
	 */
	@Test
	public void run() throws Exception {
		ZipFile apkFile = null;
		apkFile = new ZipFile(filename);
		try  {
		ZipEntry classes = apkFile.getEntry("classes.dex");
		InputStream is = apkFile.getInputStream(classes);
		try {
		    Hierarchy df = new Hierarchy(); 
		    df.parse(is);
		} finally { is.close(); }
		} finally { apkFile.close(); }
	}
	

}
