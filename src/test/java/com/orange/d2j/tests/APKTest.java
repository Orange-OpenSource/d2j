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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.options.Options;

import com.orange.d2j.DalvikResource;
import com.orange.d2j.DexFile;
import com.orange.d2j.RefNotFoundException;
import com.orange.d2j.manifest.ManifestContentHandler;


/**
 * A parameterized JUnit test that checks how a set of dex files are compiled.
 * @author piac6784
 *
 */
/**
 * @author piac6784
 *
 */
@RunWith(Parameterized.class)
public class APKTest {
	
    private static final File BASE = new File(new File(new File("src"),"test"),"cases");
	private static final File CLASSPATH = new File(BASE , "android.jar");
	private static final File APK = new File(BASE,"apk");

	/**
	 * Contains all the results found so far (memoization between "tests").
	 */
	private static Map<File, Integer> results = new HashMap<File,Integer>();
	/**
	 * Package of the wrapper class
	 */
	final static String WRAPPER_PACKAGE = "com.francetelecom.rd.fakeandroid";
	/**
	 * Name of the wrapper class.
	 */
	final static String WRAPPER_CLASS = "Wrapper";
	
	final static String WRAPPER_QUALIFIED = WRAPPER_PACKAGE + "." + WRAPPER_CLASS;

	final private static String CommonSootArgs [] = {
		"--app", "-w", "-keep-offset", "-include-all", 
		"-p","jb.a", "enabled:false",
		"-p", "cg.spark", "enabled:true,string-constants:true,cs-demand:true",		
		"-cp" 
	};

	private final static int MANIFEST_READ = 1;
	private final static int STRUCTURE_READ = 2;
	@SuppressWarnings("unused")
	private final static int METHOD_BUILT = 3;
	private final static int TYPE_OK = 4;
	private final static int POINTSTO_OK = 5;
	private final static int CONSTANT_NOT_FOUND = 6;
	
	/**
	 * Only keeps true android applications ending with .apk. 
	 * @author piac6784
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
		String folder_base = APK.getAbsolutePath();
		for(String folder: folder_base.split(":")) {
			File [] allFiles = new File(folder).listFiles(new APKFilter());
			if(allFiles == null) continue; 
			Arrays.sort(allFiles);
			for(int i=0; i < allFiles.length; i++) result.add(new Object[]{ allFiles[i] });
		}
		return result;
	}

	private File filename;
	
	private int level;

	/**
	 * This is a parameterized constructor. 
	 * @param filename
	 */
	public APKTest(File filename) {
		this.filename = filename;
	}

	/**
	 * Check the compilation of a Dex File.
	 */
	public static void run(final File filename, final APKTest test) {
		ZipFile apkFile = null;
		try {
			File tempDir = File.createTempFile("cgv", "");
			createTransientDir(tempDir);
			File jwrapper = new File(tempDir, WRAPPER_QUALIFIED + ".jimple");

			InputStream is = null;
			apkFile = new ZipFile(filename);
			String classPath = CLASSPATH + ":" + tempDir.getAbsolutePath();
			G.reset();
			String [] sootArgs = new String [ CommonSootArgs.length + 4 ];
			int pos = 0;
			System.arraycopy(CommonSootArgs,0, sootArgs,pos, CommonSootArgs.length);
			pos += CommonSootArgs.length;
			sootArgs[pos++] = classPath;
			sootArgs[pos++] = "-main-class";
			sootArgs[pos++] = WRAPPER_QUALIFIED;
			sootArgs[pos++] = WRAPPER_QUALIFIED;


			DalvikResource dr = new DalvikResource();
			ManifestContentHandler mch = new ManifestContentHandler();
			ZipEntry manifest = apkFile.getEntry("AndroidManifest.xml");
			is = apkFile.getInputStream(manifest);
			dr.setContentHandler(mch);
			dr.parse(is);
			is.close();
			test.level = MANIFEST_READ;
			writeJasminWrapper(jwrapper, mch);
			// Register all the options in soot engine.
			Options.v().parse(sootArgs);
			Scene scene = Scene.v();
			scene.loadBasicClasses();
			ZipEntry classes = apkFile.getEntry("classes.dex");
			is = apkFile.getInputStream(classes);
			DexFile df = new DexFile() {
				@Override
				protected void buildMethodsCode() throws RefNotFoundException {
					test.level = STRUCTURE_READ;
					try {
						super.buildMethodsCode();
					} catch (RefNotFoundException e) {
						test.level = CONSTANT_NOT_FOUND;
						FileWriter fstream = null;
						BufferedWriter out = null;
						try{ 
							fstream = new FileWriter("log.txt",true);
							out = new BufferedWriter(fstream);
							out.write(e.getMessage()); out.write(" -- ");
							out.write(filename.getName()); out.write("\n");
							out.close();
						} catch (IOException e2){
							e2.printStackTrace();
						} finally {
							if (fstream != null) try {fstream.close(); } catch (IOException er) { ; }
						}
						throw e;
					}
					
				}
			};
			df.parse(is);
			test.level = TYPE_OK;
			scene.loadNecessaryClasses();
			// Apply the callgraph phase.
			PackManager pm = PackManager.v();
			pm.getPack("cg").apply();
			test.level = POINTSTO_OK;
			is.close();
		} 
		catch (Throwable e) { e.printStackTrace(); }
		finally {if (apkFile != null) try { apkFile.close(); } catch(Exception e2) { } }
	}

	private void check(int wanted) {
		Integer obtained = results.get(filename);
		if (obtained == null) {
			run(filename, this);
			obtained = level;
			results.put(filename, obtained);
		}
		Assert.assertTrue(obtained >= wanted);
	}

	/**
	 * Succeed if test goes beyond reading the manifest.
	 */
	@Test
	public void manifestRead() { check(MANIFEST_READ); }
	
	/**
	 * Succeed if test goes beyond reading the structure of the class.
	 */
	@Test
	public void structureOk() { check(STRUCTURE_READ); }
		
	/**
	 * Succeed if test goes beyond typechecking
	 */
	@Test
	public void typeOk() { check(TYPE_OK); }
	
	/**
	 * Succeed if test goes beyond doing a pointsto on the class.
	 */
	@Test
	public void pointstoOk() { check(POINTSTO_OK); }
	
	static private void writeJasminWrapper(File jwrapper, ManifestContentHandler mch) throws IOException {
		String wrapper = WRAPPER_QUALIFIED;	
		PrintStream out = new PrintStream(new FileOutputStream(jwrapper), false, "US-ASCII");
		out.println("public class " + wrapper + "  extends java.lang.Object {");
		out.println("  void <init>() { ");
		out.println("      " + wrapper + " r0;");
		out.println("      r0 := @this:" + wrapper + ";");
		out.println("      specialinvoke r0.<java.lang.Object: void <init>()>();");
		out.println("      return;");
		out.println("  }");

		out.println("  public static void main(java.lang.String[])");
		out.println("  {");
		out.println("      java.lang.String[] r0;");

		Set <String> activities = mch.getActivities();
		Set <String> services = mch.getServices();
		Set <String> providers = mch.getProviders();
		Set <String> receivers = mch.getReceivers();

		int i = 0;			
		for(String activity:activities) {
			out.println("      " + activity + " a" + i++ + ";");
		}
		i = 0;
		for(String service:services) {
			out.println("      " + service + " s" + i++ + ";");
		}
		i = 0;
		for(String provider:providers) {
			out.println("      " + provider + " p" + i++ + ";");
		}
		i = 0;
		for(String receiver:receivers) {
			out.println("      " + receiver + " r" + i++ + ";");
		}
		out.println("      java.lang.Exception r5;");

		out.println("      r0 := @parameter0:java.lang.String [];");

		out.println("   label0:");
		i = 0;			
		for(String activity:activities) {
			out.println("      a"+i + " = new " + activity + ";");
			out.println("      specialinvoke a" + i + ".<" + activity + ": void <init>()> ();");
			out.println("      staticinvoke <" + WRAPPER_PACKAGE + ".android.Runtime: void runActivity(android.app.Activity)>(a" +i + ");");
			i ++;
		}
		i = 0;
		for(String service:services) {
			out.println("      s"+i + " = new " + service + ";");
			out.println("      specialinvoke s" + i + ".<" + service + ": void <init>()> ();");
			out.println("      staticinvoke <" + WRAPPER_PACKAGE + ".android.Runtime: void runService(android.app.Service)>(s" +i + ");");
			i ++;
		}
		i = 0;
		for(String provider:providers) {
			out.println("      p"+i + " = new " + provider + ";");
			out.println("      specialinvoke p" + i + ".<" + provider + ": void <init>()> ();");
			out.println("      staticinvoke <" + WRAPPER_PACKAGE + ".android.Runtime: void runProvider(android.content.ContentProvider)>(p" +i + ");");
			i ++;
		}
		i = 0;
		for(String receiver:receivers) {
			out.println("      r" + i + " = new " + receiver + ";");
			out.println("      specialinvoke r" + i + ".<" + receiver + ": void <init>()> ();");
			out.println("      staticinvoke <" + WRAPPER_PACKAGE + ".android.Runtime: void runReceiver(android.content.BroadcastReceiver)>(r" +i + ");");
			i ++;
		}
		out.println("   label1:");
		out.println("      goto label3;");
		out.println("");
		out.println("   label2:");
		out.println("      r5 := @caughtexception;");

		out.println("   label3:");
		out.println("      return;");
		out.println("      catch java.lang.Exception from label0 to label1 with label2;");
		out.println("  }");
		out.println("}");
		out.close(); 
	}

	static private void createTransientDir(final File tempDir) throws IOException {
		if (!tempDir.delete() || !tempDir.mkdir()) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe("Cannot create the temporary directory " + tempDir);
			throw new IOException("Temporary directory");
		}
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			@Override
			public void run() { deleteAll(tempDir); } 
		});
	}
	
	static private boolean deleteAll(File f) {
		if (f.isDirectory()) {
			for (File s: f.listFiles()) deleteAll(s);
		}
		return f.delete();
	}
	

}
