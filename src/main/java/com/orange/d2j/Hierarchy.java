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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import soot.Modifier;

/**
 * Represents the class hierarchy associated to the classes declared in a Dex
 * file
 * 
 * @author piac6784
 */
public class Hierarchy extends BasicDexFile {
    private String[] classes;

    private HashMap<String, ArrayList<String>> subclasses = new HashMap<String, ArrayList<String>>();

    private HashMap<String, ArrayList<String>> implementers = new HashMap<String, ArrayList<String>>();

    private String types[];

    private HashMap<String, Integer> modifiers = new HashMap<String, Integer>();

    /**
     * Emptry constructor. It is populated with an input stream.
     */
    public Hierarchy() {
    }

    @Override
    public void parse(InputStream is) throws IOException, RefNotFoundException {
        super.parse(is);
        readTypes();
        readClasses();
        reader = null;
    }

    private <K, V> void put(HashMap<K, ArrayList<V>> table, K key, V value) {
        ArrayList<V> row = table.get(key);
        if (row == null) {
            row = new ArrayList<V>();
            table.put(key, row);
        }
        row.add(value);
    }

    /**
     * Read the type index table
     */
    private void readTypes() {
        reader.seek(type_ids_off);
        types = new String[type_ids_size];
        int id;
        for (int i = 0; i < type_ids_size; i++) {
            id = reader.sint();
            types[i] = parseType(strings[id]);
        }

    }

    /**
     * Parse a single type in Java/Android format and produce a Soot type.
     * 
     * @param string
     * @return
     */
    private String parseType(String string) {
        if (string.length() < 1)
            throw new RuntimeException("parseType");
        while (true) {
            switch (string.charAt(0)) {
                case 'L':
                    int e = string.indexOf(';');
                    String name = string.substring(1, e).replace('/', '.');
                    return name;
                default:
                    return null;
            }
        }
    }

    private void readClasses() {
        int class_idx, access_flags, super_idx, itf_off;
        @SuppressWarnings("unused")
        int src_file_idx;
        classes = new String[class_defs_size];
        reader.seek(class_defs_off);
        for (int i = 0; i < class_defs_size; i++) {
            class_idx = (int) reader.uint();
            access_flags = (int) reader.uint();
            super_idx = (int) reader.uint();
            itf_off = (int) reader.uint();
            reader.uint();
            reader.uint();
            reader.uint(); // clas offset
            reader.uint();
            String name = types[class_idx];
            if (name == null) {
                D2JLog.warning("Did not find a class at index " + class_idx);
                continue;
            }
            modifiers.put(name, access_flags);
            String superClass = null;
            if (super_idx != NO_INDEX) {
                superClass = types[super_idx];
                if (superClass == null) {
                    D2JLog.warning("Did not find a class for supper of " + name);
                    continue;
                } else
                    put(subclasses, superClass, name);

            }
            readTypeList(name, itf_off);
        }

    }

    private void readTypeList(String name, int off) {
        if (off != 0) {
            int saved = reader.getPos();
            reader.seek(off);
            int size = (int) reader.uint();
            for (int i = 0; i < size; i++) {
                int idx = reader.ushort();
                String itf = types[idx];
                if (itf == null) {
                    D2JLog.warning("Did not find an interface for " + name + " at " + idx);
                } else {
                    put(implementers, itf, name);
                }
            }
            reader.seek(saved);
        }
    }

    /**
     * Entry for testing.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        InputStream is;
        ZipFile apkFile = null;
        String filename = args[0];
        try {
            if (filename.endsWith(".apk")) {
                apkFile = new ZipFile(filename);
                ZipEntry classes = apkFile.getEntry("classes.dex");
                if (classes == null) {
                    throw new Exception("no classes");
                }
                is = apkFile.getInputStream(classes);
            } else {
                is = new FileInputStream(filename);
            }
            try {
                Hierarchy df = new Hierarchy();
                df.parse(is);
            } finally {
                is.close();
            }
        } finally {
            if (apkFile != null) {
                apkFile.close();
            }
        }
    }

    /**
     * Gives back the direct implementers.
     * 
     * @param itf
     * @return
     */
    public ArrayList<String> implementers(String itf) {
        return implementers.get(itf);
    }

    /**
     * Given the name of a class gives back all its sub classes
     * 
     * @param clazz as a classname
     * @return as a set of class names.
     */
    public HashSet<String> subclasses(String clazz) {
        HashSet<String> result = new HashSet<String>();
        Stack<String> todo = new Stack<String>();
        todo.push(clazz);
        while (!todo.isEmpty()) {
            String cl = todo.pop();
            ArrayList<String> subclasses = directSubclasses(cl);
            if (subclasses != null) {
                for (String scl : subclasses) {
                    if (!result.contains(scl)) {
                        result.add(scl);
                        todo.push(scl);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Gives back the direct subclasses.
     * 
     * @param clazz as a classname
     * @return as a set of classnames
     */
    public ArrayList<String> directSubclasses(String clazz) {
        return subclasses.get(clazz);
    }

    /**
     * Check if a name is declared as an interface in the APK.
     * 
     * @param clazz the name of the class
     * @return true if the class is declared as interface in the APK
     */
    public boolean isInterface(String clazz) {
        Integer mod = modifiers.get(clazz);
        return !(mod == null) && ((mod & Modifier.INTERFACE) != 0);
    }

    /**
     * Check if a name is declared as abstract.
     * 
     * @param clazz
     * @return
     */
    public boolean isAbstract(String clazz) {
        Integer mod = modifiers.get(clazz);
        boolean result = !(mod == null) && ((mod & Modifier.ABSTRACT) != 0);
        return result;
    }

    /**
     * All the entries of the APK
     * 
     * @return as an array of classnames.
     */
    public String[] contents() {
        return Arrays.copyOf(classes, classes.length);
    }
}
