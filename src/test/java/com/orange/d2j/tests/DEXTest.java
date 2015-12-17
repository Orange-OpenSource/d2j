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
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import soot.Body;
import soot.G;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.util.Chain;

import com.orange.d2j.DexFile;

/**
 * A parameterized JUnit test that checks how a set of dex files are compiled.
 * @author Pierre Cregut
 *
 */
@RunWith(Parameterized.class)
public class DEXTest {
	
	
    private static final File BASE = new File(new File(new File("src"),"test"),"cases");
	private static final File CLASSPATH = new File(BASE , "android.jar");
	private static final File DEX =  new File(BASE,"dex");
    private static final File XML = new File(BASE,"xml");
    
	/**
	 * Only keeps test ending with .dex. Necessary because of .svn folder.
	 * @author Pierre Cregut
	 *
	 */
	private static class DexFilter implements FileFilter {
		@Override
		public boolean accept(File arg) { return arg.getName().endsWith(".dex"); }

	}
	/**
	 * Builds the list of all dex test files (in folder dex).
	 * @return
	 */
	@Parameters(name="{index}:{0}")
	public static List <Object []> filenames() {
		File [] allFiles = DEX.listFiles(new DexFilter());
		Arrays.sort(allFiles);
		int l = allFiles.length;
		Object [] [] result = new Object [l] [1];
		for(int i=0; i < l; i++) result[i][0] = allFiles[i];
		return Arrays.asList(result);
	}

	private File filename;
	/**
	 * This is a parameterized constructor. 
	 * @param filename
	 */
	public DEXTest(File filename) {
		this.filename = filename;
	}

	/**
	 * Check the compilation of a Dex File.
	 */
	@Test
	public void run() throws Exception {
        G.reset();
        InputStream is = new FileInputStream(filename);
        try {
            Scene.v().setSootClassPath(CLASSPATH.getAbsolutePath());
            Scene.v().loadBasicClasses();
            DexFile df = new DexFile();
            df.parse(is);
            check(Scene.v(), parseXML(filename));
        } finally {
            is.close();
        }
	}

	private List<Element> sons(Element n) {
		NodeList list = n.getChildNodes();
		int l = list.getLength();
		List <Element> r = new ArrayList <Element> ();
		for(int i=0; i < l; i++) 
			if (list.item(i) instanceof Element) r.add((Element) list.item(i));
		return r;
	}

	private Element parseXML(File dexFile) throws Exception {
		
		File f = new File(XML, dexFile.getName().replace(".dex",".xml"));
		if (!f.exists()) return null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docbuild = factory.newDocumentBuilder();
		Element root = docbuild.parse(f).getDocumentElement();
		return root;
	}

	private void check(Object o, Element e) throws Exception {
		if (e == null) return;
		for(Element acc : sons(e)) {
			Object r = null;
			String expectedClass = null;
			String tag = acc.getTagName();
			if (tag.equals("e") || tag.equals("v")) {
				String method = acc.getAttribute("method");
				if (acc.hasAttribute("arg")) {
					String arg = acc.getAttribute("arg");
					if (arg.length() > 0 && Character.isDigit(arg.charAt(0))) {
						Integer i = Integer.decode(arg);
						Method m = o.getClass().getMethod(method, Integer.class);
						r = m.invoke(o,i);
					} else {
						Method m = o.getClass().getMethod(method, String.class);
						r = m.invoke(o,arg);
					}
				} else {
					try {
						Method m = o.getClass().getMethod(method);
						r = m.invoke(o);
					} catch (NoSuchMethodException exc) {
						r = null;
						Assert.assertTrue(o + "contains" + method, false);
					}

				}

				if (acc.hasAttribute("int")) {
					Integer expected = Integer.decode(acc.getAttribute("int"));
					Assert.assertTrue(r + "!=" + expected, r.equals(expected));
				} else if (acc.hasAttribute("string")) {
					String expected = acc.getAttribute("string");
					Assert.assertTrue(r + "!=" + expected, r.equals(expected) || ((String) r).startsWith(expected + "#"));
				} else {
					if (acc.hasAttribute("class")) {
						expectedClass = acc.getAttribute("class");
						if (! expectedClass.contains(".")) {
							if (tag.equals("v")) expectedClass = "soot.jimple.internal." + expectedClass;
							else expectedClass = "soot." + expectedClass;
						}
					}
				}
			} else if (acc.getTagName().equals("i")) {
				Assert.assertTrue("" + o, o instanceof SootMethod);
				Assert.assertTrue(((SootMethod) o).hasActiveBody());
				int pos = Integer.parseInt(acc.getAttribute("pos"));
				Body b = ((SootMethod) o).getActiveBody();
				Chain <Unit> lu = b.getUnits();
				Unit u = lu.getFirst();
				for(int i = 0; i < pos; i++) u = lu.getSuccOf(u);
				r = u;
				if (acc.hasAttribute("class")) {
					expectedClass = acc.getAttribute("class");
					if (! expectedClass.contains(".")) expectedClass = "soot.jimple.internal." + expectedClass;
				}
			}
			Assert.assertTrue(r != null);
			if (expectedClass != null) {
				Assert.assertTrue("!" + r.getClass() + " instanceof " + expectedClass , 
						Class.forName(expectedClass).isAssignableFrom(r.getClass()));
			}
			check(r,acc);
		}
	}
}
