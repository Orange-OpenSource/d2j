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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xml.sax.SAXException;

import com.orange.d2j.manifest.ManifestContentHandler;

import soot.RefType;
import soot.SootFieldRef;
import soot.SootMethodRef;

/**
 * Utility function to handle an Android packaged application (or APK).
 * 
 * @author piac6784
 */
public class APKFile {

    /** The Constant CODE. */
    protected static final String CODE = "classes.dex";

    /** The Constant MANIFEST. */
    protected static final String MANIFEST = "AndroidManifest.xml";

    /** The Constant MAGIC_TEMPLATE. */
    static final byte[] MAGIC_TEMPLATE = {
            0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00
    };

    /** The file. */
    final private File file;

    /** The apk file. */
    private ZipFile apkFile;

    /** The dex. */
    protected DexFile dex = null;

    /** The hier. */
    protected Hierarchy hier = null;

    /** The manifest. */
    protected ManifestContentHandler manifest = null;

    /**
     * Constructor from the name of the file containing the APK.
     * 
     * @param file the file
     */
    public APKFile(File file) {
        this.file = file;
    }

    /**
     * Gives back the name of the file.
     * 
     * @return the name
     */
    public String getName() {
        return file.getAbsolutePath();
    }

    /**
     * Constructor for extensions only. The field is not used here.
     */
    protected APKFile() {
        this.file = null;
    }

    /**
     * Check that the package is well-formed (at least a classes.dex and a
     * manifest) and that the version is handled by the application.
     * 
     * @return true, if successful
     */
    public boolean checkVersion() {
        DataInputStream is = null;
        ZipFile apkFile = null;
        boolean result;
        try {
            try {
                apkFile = new ZipFile(file);
            } catch (IOException e) {
                D2JLog.warning("Cannot open APK " + file);
                throw e;

            }
            try {
                ZipEntry manifest = apkFile.getEntry(MANIFEST);
                if (manifest == null) {
                    D2JLog.warning("No manifest in file " + file);
                    throw new IOException("No manifest file");
                }
                ZipEntry classes = apkFile.getEntry(CODE);
                if (classes == null) {
                    D2JLog.warning("No code in file " + file);
                    throw new IOException("No code");
                }
                try {
                    is = new DataInputStream(apkFile.getInputStream(classes));
                    try {
                        byte[] magic = new byte[8];
                        is.readFully(magic);
                        result = Arrays.equals(magic, MAGIC_TEMPLATE);
                    } finally {
                        is.close();
                    }

                } catch (IOException e) {
                    D2JLog.warning("IO exception while checking code version in " + file);
                    throw e;
                }
            } finally {
                apkFile.close();
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }

    /**
     * Gives back a handle to the code of the APK.
     * 
     * @return the input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public InputStream codeStream() throws IOException {
        if (apkFile == null)
            apkFile = new ZipFile(file);
        ZipEntry classes = apkFile.getEntry(CODE);
        return classes == null ? null : apkFile.getInputStream(classes);
    }

    /**
     * Gives back a handle to the manifest of the APK.
     * 
     * @return the input stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public InputStream manifestStream() throws IOException {
        if (apkFile == null)
            apkFile = new ZipFile(file);
        ZipEntry manifest = apkFile.getEntry(MANIFEST);
        return manifest == null ? null : apkFile.getInputStream(manifest);
    }

    /**
     * Forget the code. May be necessary if several analysis are done.
     */
    public void resetCode() {
        dex = null;
    }

    /**
     * Access (with memoization) to the code.
     * 
     * @return the dex file
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public DexFile code() throws IOException {
        if (dex == null) {
            InputStream is = codeStream();
            if (is == null) throw new IOException("no code stream");
            try {
                dex = new DexFile();
                try {
                    dex.parse(is);
                } catch (RefNotFoundException e) {
                    throw new IOException(e);
                }
            } finally {
                is.close();
            }
        }
        return dex;
    }

    /**
     * Gives back teh hierarchy corresponding to the APK file.
     * 
     * @return the hierarchy
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public Hierarchy hierarchy() throws IOException {
        if (hier == null) {
            InputStream is = codeStream();
            if (is == null) throw new IOException("no code stream");
            try {
                hier = new Hierarchy();
                try {
                    hier.parse(is);
                } catch (RefNotFoundException e) {
                    throw new IOException(e);
                }
            } finally {
                is.close();
            }
        }
        return hier;
    }

    /**
     * Gives access to the Android manifest through an abstract version of it.
     * 
     * @return the manifest content handler
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public ManifestContentHandler manifest() throws IOException {
        if (manifest == null) {
            InputStream is = manifestStream();
            if (is == null) {
                return null;
            }
            DalvikResource dr = new DalvikResource();
            manifest = new ManifestContentHandler();
            try {
                dr.setContentHandler(manifest);
                dr.parse(is);
            } catch (SAXException e) {
                throw new IOException("SAX Parsing:" + e.getMessage());
            } finally {
                is.close();
            }
        }
        return manifest;
    }

    /**
     * Gives back the list of methods that are used in the APK but are defined
     * neither in the profile nor in the APK.
     * 
     * @return A set of method references in soot format
     */
    public Set<SootMethodRef> unresolvedMethods() {
        if (dex != null)
            return dex.unresolvedMethods;
        return null;
    }

    /**
     * Gives back the list of unresolved fields as used in the APK but not
     * defined in the profile or the APK.
     * 
     * @return A set of field references in Soot format
     */
    public Set<SootFieldRef> unresolvedFields() {
        if (dex != null)
            return dex.unresolvedFields;
        return null;
    }

    /**
     * Gives back the list of unresolved classes as used in the APK but not
     * defined in the profile or the APK.
     * 
     * @return A set of reference types in Soot format
     */
    public Set<RefType> unresolvedClasses() {
        if (dex != null)
            return dex.unresolvedClasses;
        return null;
    }
}
