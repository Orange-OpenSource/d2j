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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.Hierarchy;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.VoidType;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.DoubleConstant;
import soot.jimple.FloatConstant;
import soot.jimple.IntConstant;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.StringConstant;
import soot.tagkit.AbstractHost;
import soot.tagkit.AnnotationAnnotationElem;
import soot.tagkit.AnnotationArrayElem;
import soot.tagkit.AnnotationClassElem;
import soot.tagkit.AnnotationDoubleElem;
import soot.tagkit.AnnotationElem;
import soot.tagkit.AnnotationEnumElem;
import soot.tagkit.AnnotationFloatElem;
import soot.tagkit.AnnotationIntElem;
import soot.tagkit.AnnotationLongElem;
import soot.tagkit.AnnotationStringElem;
import soot.tagkit.AnnotationTag;
import soot.tagkit.DoubleConstantValueTag;
import soot.tagkit.FloatConstantValueTag;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.LongConstantValueTag;
import soot.tagkit.StringConstantValueTag;
import soot.tagkit.Tag;
import soot.toolkits.exceptions.ThrowAnalysis;
import soot.toolkits.exceptions.UnitThrowAnalysis;

/**
 * Reads a DEX file and builds a representation of this file.
 * 
 * @author piac6784
 */
public class DexFile extends BasicDexFile {
    /*
     * public final static int ACC_PUBLIC = 0x1; public final static int
     * ACC_PRIVATE = 0x2; public final static int ACC_PROTECTED = 0x4; public
     * final static int ACC_STATIC = 0x8; public final static int ACC_FINAL =
     * 0x10; public final static int ACC_VOLATILE = 0x40; public final static
     * int ACC_BRIDGE = 0x40; public final static int ACC_TRANSIENT = 0x80;
     * public final static int ACC_VARARGS = 0x80; public final static int
     * ACC_NATIVE = 0x100; public final static int ACC_INTERFACE = 0x200; public
     * final static int ACC_ABSTRACT = 0x400; public final static int ACC_STRICT
     * = 0x800; public final static int ACC_SYNTHETIC = 0x1000; public final
     * static int ACC_ANNOTATION = 0x2000; public final static int ACC_ENUM =
     * 0x4000; public final static int ACC_CONSTRUCTOR = 0x10000;
     */

    private static final String REDEFINED_PACKAGE_PREFIX = "redefined.";

    /**
     * Constant representing the absence of an index in a table especially for
     * super class.
     */
    public final static int NO_INDEX = 0xFFFFFFFF;

    private final Scene scene = Scene.v();

    /**
     * The type constant pool
     */
    private Type[] types;

    /**
     * The prototype constant pool
     */
    MethodProto[] protos;

    /**
     * Soot Fields pool table
     */
    SootField[] fields;

    /**
     * The class containing the fields in the field pool . May not be hooked to
     * it when created.
     */
    RefType[] field_owner;

    /**
     * Soot methods pool table
     */
    SootMethod[] methods;

    /**
     * The class containing the methods in the field pool . May not be hooked to
     * it when created.
     */
    RefType[] method_owner;

    /**
     * Soot classes pool table
     */
    SootClass[] classes;

    /**
     * Accumulator for methods referenced without counter part
     */
    Set<SootMethodRef> unresolvedMethods = new HashSet<SootMethodRef>();

    /**
     * Accumulator for fields referenced without counter part
     */
    Set<SootFieldRef> unresolvedFields = new HashSet<SootFieldRef>();

    /**
     * Accumulator for classes referenced without counter part
     */
    Set<RefType> unresolvedClasses = new HashSet<RefType>();

    Set<String> redefined = new HashSet<String>();

    private HashMap<SootMethod, Integer> builders = new HashMap<SootMethod, Integer>();

    /**
     * Constructor. Needs the classpath to find Android libraries.
     * 
     * @param classpath
     */
    public DexFile() {
        Scene scene = Scene.v();
        scene.setPhantomRefs(true);
        patchThrowAnalysis(scene);
    }

    /**
     * Replace the throw analysis in a scene so that it pretends return cannot
     * throw exceptions.
     * 
     * @param scene
     */
    public static void patchThrowAnalysis(Scene scene) {
        ThrowAnalysis defaultAnalysis = scene.getDefaultThrowAnalysis();
        if (!(defaultAnalysis instanceof DalvikThrowAnalysis)) {
            scene.setDefaultThrowAnalysis(new DalvikThrowAnalysis(UnitThrowAnalysis.v()));
        }
    }

    /**
     * Takes an input stream coresponding to a dex file and populate the
     * structure.
     * 
     * @param is
     * @throws IOException
     * @throws RefNotFoundException
     */
    @Override
    public void parse(InputStream is) throws IOException, RefNotFoundException {
        super.parse(is);
        readTypes();
        // If a class is redefined, move its package name.
        escapeClassRedefinition();
        // pre declare the classes defined in APK
        readPreClasses();
        // check that all types are solved. If not introduce phantom classes.
        resolveTypes();
        readPrototypes();
        readFields();
        readMethods();
        readClasses();
        reader = null;
    }

    /**
     * Read the type index table
     */
    private void readTypes() {
        reader.seek(type_ids_off);
        types = new Type[type_ids_size];
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
    private Type parseType(String string) {
        int asize = 0;
        int i = 0;
        if (string.length() < 1)
            throw new RuntimeException("parseType");
        while (true) {
            switch (string.charAt(i)) {
                case 'V':
                    return VoidType.v();
                case 'B':
                    return wrapInArray(ByteType.v(), asize);
                case 'Z':
                    return wrapInArray(BooleanType.v(), asize);
                case 'S':
                    return wrapInArray(ShortType.v(), asize);
                case 'C':
                    return wrapInArray(CharType.v(), asize);
                case 'I':
                    return wrapInArray(IntType.v(), asize);
                case 'J':
                    return wrapInArray(LongType.v(), asize);
                case 'F':
                    return wrapInArray(FloatType.v(), asize);
                case 'D':
                    return wrapInArray(DoubleType.v(), asize);
                case 'L':
                    int e = string.indexOf(';');
                    String name = string.substring(i + 1, e).replace('/', '.');
                    return wrapInArray(RefType.v(name), asize);
                case '[':
                    asize++;
                    i++;
                    break;
                default:
                    throw new RuntimeException("parseType : " + i + " " + string);
            }
        }
    }

    /**
     * Produce an array type if necessary or keep the type if size is 0
     * 
     * @param v
     * @param i
     * @return
     */
    private Type wrapInArray(Type v, int i) {
        if (i == 0)
            return v;
        return ArrayType.v(v, i);
    }

    /**
     * Read the method prototypes table
     */
    private void readPrototypes() {
        int ret_idx, off;
        Type retType;
        List<Type> params;
        reader.seek(proto_ids_off);
        protos = new MethodProto[proto_ids_size];
        for (int i = 0; i < proto_ids_size; i++) {
            reader.uint(); // shorty_idx
            ret_idx = (int) reader.uint();
            retType = findType(ret_idx);
            off = (int) reader.uint();
            params = readTypeList(off);
            protos[i] = new MethodProto(params, retType);
        }
    }

    private List<Type> readTypeList(int off) {
        List<Type> typeList = new ArrayList<Type>();
        if (off != 0) {
            int saved = reader.getPos();
            reader.seek(off);
            int size = (int) reader.uint();
            for (int i = 0; i < size; i++)
                typeList.add(findType(reader.ushort()));
            reader.seek(saved);
        }
        return typeList;
    }

    /**
     * Read the field index table.
     */
    private void readFields() {
        int class_idx, type_idx, name_idx;
        fields = new SootField[field_ids_size];
        field_owner = new RefType[field_ids_size];
        reader.seek(field_ids_off);
        for (int i = 0; i < field_ids_size; i++) {
            class_idx = reader.ushort();
            type_idx = reader.ushort();
            name_idx = (int) reader.uint();
            RefType t;
            SootClass c = null;
            Type typ = findType(class_idx);
            if (!(typ instanceof RefType)) {
                D2JLog.warning("Problem not a RefType at " + class_idx);
                t = null;
            } else {
                t = (RefType) typ;
                c = scene.tryLoadClass(t.getClassName(), SootClass.BODIES);
            }
            Type type = findType(type_idx);
            String name = strings[name_idx];
            SootField f = (c != null) ? getField(c, name, type) : new SootField(name, type);
            if (f == null) {
                D2JLog.warning("Creating a phantom field " + name + " " + type + " in " + c);
                f = new SootField(name, type);
            }
            fields[i] = f;
            field_owner[i] = t;
        }
    }

    /**
     * Read the method index table
     */
    private void readMethods() {
        int class_idx, proto_idx, name_idx;
        MethodProto proto;
        RefType objectType = scene.getRefType("java.lang.Object");
        methods = new SootMethod[method_ids_size];
        method_owner = new RefType[method_ids_size];
        reader.seek(method_ids_off);
        for (int i = 0; i < method_ids_size; i++) {
            class_idx = reader.ushort();
            proto_idx = reader.ushort();
            proto = protos[proto_idx];
            name_idx = (int) reader.uint();
            Type raw_t = findType(class_idx);
            RefType t = (raw_t instanceof ArrayType) ? objectType
                    : (raw_t instanceof RefType) ? (RefType) raw_t : null;
            SootClass c = (t == null) ? null : scene.tryLoadClass(t.getClassName(),
                    SootClass.BODIES);
            String name = strings[name_idx];
            SootMethod m = (c != null) ? getMethod(c, name, proto.params, proto.ret)
                    : new SootMethod(name, proto.params, proto.ret);
            if (m == null) {
                D2JLog.warning("Warning creating a phantom method " + name + proto.params + " in "
                        + c);
                m = new SootMethod(name, proto.params, proto.ret);
            }
            methods[i] = m;
            method_owner[i] = t;
            if (D2JLog.classes)
                D2JLog.info("Method " + i + ": " + methods[i].getSubSignature() + " ["
                        + method_owner[i] + "]");
        }
    }

    /**
     * Access to a method in the Soot scene for a given class and a given
     * signature. It will look in super classes to find it if it is not defined
     * at the level of this class.
     * 
     * @param c the class we look in
     * @param name the name of the method
     * @param params the types of the parameters
     * @param ret the return type.
     * @return the method definition in the scene.
     */
    private SootMethod getMethod(SootClass c, String name, List<Type> params, Type ret) {
        if (D2JLog.classes)
            D2JLog.info("Trying to find method " + name + params + ":" + ret + " in " + c);
        SootMethod result;
        try {
            try {
                return c.getMethod(name, params, ret);
            } catch (RuntimeException e) {
                try {
                    return c.getMethod(name, params);
                } catch (RuntimeException e1) {
                    return c.getMethod(name);
                }
            }
        } catch (RuntimeException e1) {
            if (c.isInterface() || c.isAbstract()) {
                for (SootClass itf : c.getInterfaces()) {
                    result = getMethod(itf, name, params, ret);
                    if (result != null)
                        return result;
                }
            }
            if (c.hasSuperclass()) {
                return getMethod(c.getSuperclass(), name, params, ret);
            } else
                return null;
        }
    }

    /**
     * Gives back a reference to a soot method corresponding to the ith entry
     * 
     * @param i position of entry
     * @param isStatic if identified as static.
     * @return
     */
    @SuppressWarnings("unchecked")
    public SootMethodRef getMethodRef(int i, boolean isStatic) {
        RefType owner = (RefType) method_owner[i];
        SootMethod placeholder = methods[i];
        String name = placeholder.getName();
        Type returnType = placeholder.getReturnType();
        List<Type> argsTypes = placeholder.getParameterTypes();
        return Scene.v().makeMethodRef(owner.getSootClass(), name, argsTypes, returnType, isStatic);
    }

    /**
     * Access to a method by its index in the DEX method pool. Everything is
     * done to find it in the Soot Scene.
     * 
     * @param i
     * @return
     */
    @SuppressWarnings("unchecked")
    public SootMethod getMethod(int i) throws RefNotFoundException {
        RefType owner = (RefType) method_owner[i];
        SootMethod placeholder = methods[i];
        String name = placeholder.getName();
        Type returnType = placeholder.getReturnType();
        List<Type> argsTypes = placeholder.getParameterTypes();
        SootMethod result = getMethod(owner.getSootClass(), name, argsTypes, returnType);
        if (result == null) {
            D2JLog.info("Cannot find correct witness for method at index " + i + " (" + name
                    + " in " + owner + ")");
            throw new RefNotFoundException("Method " + returnType + " " + name + argsTypes + " in "
                    + owner);
        }
        return result;
    }

    /**
     * Access to a method in the Soot scene for a given class and a given
     * signature. It will look in super classes to find it if it is not defined
     * at the level of this class.
     * 
     * @param c the class containing the field
     * @param name the field name
     * @param typ the type of of the field
     * @return the soot field
     */
    private SootField getField(SootClass c, String name, Type typ) {
        try {
            return c.getField(name, typ);
        } catch (RuntimeException e) {
            if (c.isInterface() || c.isAbstract()) {
                for (SootClass itf : c.getInterfaces()) {
                    SootField result = getField(itf, name, typ);
                    if (result != null)
                        return result;
                }
            }
            if (c.hasSuperclass()) {
                return getField(c.getSuperclass(), name, typ);
            } else
                return null;
        }
    }

    /**
     * Access to a field by its index in the DEX field pool. Everything is done
     * to find it in the Soot Scene.
     * 
     * @param i
     * @return
     */
    public SootField getField(int i) throws RefNotFoundException {
        SootField placeholder = fields[i];
        SootField result = getField(((RefType) field_owner[i]).getSootClass(),
                placeholder.getName(), placeholder.getType());
        if (result == null) {
            D2JLog.info("Cannot find correct witness for field at index " + i + " ("
                    + placeholder.getName() + " in " + field_owner[i] + ")");
            throw new RefNotFoundException("Field " + placeholder.getType() + " "
                    + placeholder.getName() + " in " + field_owner[i]);
        }
        return result;
    }

    /**
     * Gives back a reference to a soot field corresponding to the ith entry
     * 
     * @param i position of entry
     * @param isStatic if identified as static.
     * @return
     */
    public SootFieldRef getFieldRef(int i, boolean isStatic) {
        SootClass c = ((RefType) field_owner[i]).getSootClass();
        SootField def = fields[i];
        return Scene.v().makeFieldRef(c, def.getName(), def.getType(), isStatic);
    }

    private void readPreClasses() {
        int class_idx, access_flags;
        classes = new SootClass[class_defs_size];
        reader.seek(class_defs_off);
        for (int i = 0; i < class_defs_size; i++) {
            class_idx = (int) reader.uint();
            access_flags = (int) reader.uint();
            reader.skip(6 * 4); // Skip 6 uint fields.
            SootClass clazz;
            Type raw_type = types[class_idx];
            if (!(raw_type instanceof RefType)) {
                D2JLog.warning("Did not find a class at index " + class_idx + ": " + raw_type);
                continue;
            }
            RefType typ = (RefType) raw_type;
            String name = typ.getClassName();
            clazz = new SootClass(name, dalvikToJimple(access_flags));
            Scene.v().addClass(clazz);
            if (D2JLog.classes)
                D2JLog.info("Class " + clazz + " added.");
            clazz.setApplicationClass();
            classes[i] = clazz;
        }
        // Here we have all the classes. We need to make sure everything is
        // loaded. Class mentioned
        // in the APK are mentionned in the types array. We need to explore it.
        // Array ?
        for (Type typ : types) {
            if (typ instanceof RefType) {
                RefType rf = (RefType) typ;
                scene.tryLoadClass(rf.getClassName(), SootClass.BODIES);
                SootClass c = rf.getSootClass();
                if (c.resolvingLevel() == SootClass.DANGLING) {
                    c.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
                    c.setResolvingLevel(SootClass.BODIES);
                    try {
                        scene.addClass(c);
                    } catch (RuntimeException e) {
                    }
                    if (c.getName().startsWith("dalvik.annotation."))
                        continue; // TODO better way to handle it
                    unresolvedClasses.add(rf);
                }
            }
        }
        if (unresolvedClasses.size() > 0) {
            scene.setActiveHierarchy(new Hierarchy());
        }
    }

    /**
     * Parse class definitions in DEX file.
     * 
     * @throws RefNotFoundException
     */
    private void readClasses() throws RefNotFoundException {
        int super_idx, itf_off, class_off;
        int static_values_off;
        reader.seek(class_defs_off);
        for (int i = 0; i < class_defs_size; i++) {
            reader.uint(); // class_idx done in readPreClasses.
            reader.uint(); // access_flags
            super_idx = (int) reader.uint();
            itf_off = (int) reader.uint();
            reader.uint();// src_file_idx
            reader.uint(); // annotation offset
            class_off = (int) reader.uint();
            static_values_off = (int) reader.uint();
            Constant[] static_values = parse_static_values(static_values_off);
            SootClass clazz = classes[i];
            if (super_idx != NO_INDEX) {
                Type raw_type = findType(super_idx);
                if (!(raw_type instanceof RefType)) {
                    D2JLog.warning("Did not find a class for super of " + clazz.getName() + ": "
                            + raw_type);
                    continue;
                }
                RefType superType = (RefType) raw_type;
                SootClass superClass = superType.getSootClass();
                clazz.setSuperclass(superClass);
            }

            List<Type> itfTypes = readTypeList(itf_off);
            for (Type itfType : itfTypes) {
                SootClass itfClass = ((RefType) itfType).getSootClass();
                clazz.addInterface(itfClass);
                itfClass.setModifiers(itfClass.getModifiers() | Modifier.INTERFACE);
            }
            parseClass(clazz, class_off, static_values);
            classes[i] = clazz;
            // parseAnnotations(clazz,annotations_off);
        }

        buildMethodsCode();
    }

    /**
     * This function renames the package of internally defined methods that
     * conflict with the system. The main problem is that we do not have a
     * classical class loader to resolve loading because regular class file
     * loading and dex file loading are completely separate.
     */

    private void escapeClassRedefinition() {
        reader.seek(class_defs_off);
        for (int i = 0; i < class_defs_size; i++) {
            int class_idx = (int) reader.uint();
            reader.seek(reader.getPos() + 7 * 4);
            Type raw_type = types[class_idx];
            if (!(raw_type instanceof RefType)) {
                D2JLog.warning("Did not find a class at index " + class_idx + ": " + raw_type);
                continue;
            }
            RefType typ = (RefType) raw_type;
            String name = typ.getClassName();
            if (scene.tryLoadClass(name, SootClass.BODIES) != null) {
                D2JLog.warning("Redefinition of class " + name);
                redefined.add(name);
            }
        }
        if (redefined.size() > 0) {
            for (int i = 0; i < types.length; i++) {
                Type t = types[i];
                if (t instanceof RefType) {
                    String name = ((RefType) t).getClassName();
                    if (redefined.contains(name)) {
                        types[i] = RefType.v(REDEFINED_PACKAGE_PREFIX + name);
                    }
                } else if (t instanceof ArrayType) {
                    ArrayType at = (ArrayType) t;
                    if (at.baseType instanceof RefType) {
                        String name = ((RefType) at.baseType).getClassName();
                        if (redefined.contains(name)) {
                            types[i] = ArrayType.v(RefType.v(REDEFINED_PACKAGE_PREFIX + name),
                                    at.numDimensions);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method must parse the annotations of the class. We are mostly
     * interested in System annotations.
     * 
     * @param clazz
     * @param annotationsOff
     */
    protected void parseAnnotations(SootClass clazz, int offset) {
        if (offset == 0)
            return;
        int save = reader.getPos();
        reader.seek(offset);
        int clazz_annot_offset = (int) reader.uint();
        int fields_annot_size = (int) reader.uint();
        int methods_annot_size = (int) reader.uint();
        int params_annot_size = (int) reader.uint();
        for (int i = 0; i < fields_annot_size; i++) {
            int field_idx = (int) reader.uint();
            int annot_off = (int) reader.uint();
            SootField proto = fields[field_idx];
            try {
                SootField field = clazz.getField(proto.getName(), proto.getType());
                parseAnnotationSet(annot_off, field);
            } catch (RuntimeException e) {
                D2JLog.warning("Field " + proto.getSubSignature() + " unknown in " + clazz);
            }
        }
        for (int i = 0; i < methods_annot_size; i++) {
            int method_idx = (int) reader.uint();
            int annot_off = (int) reader.uint();
            SootMethod proto = methods[method_idx];
            try {
                SootMethod meth = clazz.getMethod(proto.getName(), proto.getParameterTypes(),
                        proto.getReturnType());
                parseAnnotationSet(annot_off, meth);
            } catch (RuntimeException e) {
                D2JLog.warning("method " + proto.getSubSignature() + " unknown in " + clazz);
            }
        }
        for (int i = 0; i < params_annot_size; i++) {
            int method_idx = (int) reader.uint();
            int annot_off = (int) reader.uint();
            SootMethod proto = methods[method_idx];
            try {
                SootMethod meth = clazz.getMethod(proto.getName(), proto.getParameterTypes(),
                        proto.getReturnType());
                parseAnnotationSetSet(annot_off, meth);
            } catch (RuntimeException e) {
                D2JLog.warning("method " + proto.getSubSignature() + " unknown in " + clazz);
            }
        }
        parseAnnotationSet(clazz_annot_offset, clazz);
        reader.seek(save);
    }

    private void parseAnnotationSet(int offset, AbstractHost host) {
        if (offset == 0)
            return;
        int save = reader.getPos();
        reader.seek(offset);
        int size = (int) reader.uint();

        for (int i = 0; i < size; i++) {
            int offAnnot = (int) reader.uint();
            AnnotationTag tag = parseVisibleAnnotation(offAnnot);
            host.addTag(tag);
        }
        reader.seek(save);
    }

    private void parseAnnotationSetSet(int offset, SootMethod hostMethod) {
        if (offset == 0)
            return;
        int save = reader.getPos();
        reader.seek(offset);
        reader.uint(); // size
        // TODO Rework completely with notion of VisibilityParameterAnnotation
        reader.seek(save);
    }

    private AnnotationTag parseVisibleAnnotation(int offset) {
        // TODO Rework completely with notion of VisibilityAnnotation
        int save = reader.getPos();
        reader.seek(offset);
        reader.ubyte(); // visibility
        AnnotationTag tag = parseAnnotation();
        reader.seek(save);
        return tag;
    }

    private AnnotationTag parseAnnotation() {
        int typ_idx = (int) reader.uleb128();
        int size = (int) reader.uleb128();
        RefType typ = (RefType) findType(typ_idx);
        AnnotationTag tag = new AnnotationTag(typ.getClassName(), size);
        ArrayList<AnnotationElem> elems = new ArrayList<AnnotationElem>();
        for (int i = 0; i < size; i++) {
            int name_idx = (int) reader.uleb128();
            String name = strings[name_idx];
            AnnotationElem e = parseAnnotationElem(name);
            elems.add(e);
        }
        tag.setElems(elems);
        return tag;
    }

    private AnnotationElem parseAnnotationElem(String name) {
        int arg_type = reader.ubyte();
        int arg = (arg_type >> 5) & 0x7;
        switch (arg_type & 0x1f) {
            case 0:
                return new AnnotationIntElem(reader.sbyte(), 'B', name);
            case 2:
                return new AnnotationIntElem(arg == 0 ? reader.sbyte() : reader.sshort(), 'S', name);
            case 3:
                return new AnnotationIntElem(arg == 0 ? reader.ubyte() : reader.ushort(), 'C', name);
            case 4:
                return new AnnotationIntElem((int) DalvikValueReader.completeSignSizedLong(
                        reader.sizedLong(arg), arg), 'I', name);
            case 6:
                return new AnnotationLongElem(DalvikValueReader.completeSignSizedLong(
                        reader.sizedLong(arg), arg), 'J', name);
            case 0x10:
                return new AnnotationFloatElem(
                        Float.intBitsToFloat((int) reader.sizedLong(arg) << (8 * (3 - arg))), 'F',
                        name);
            case 0x11:
                return new AnnotationDoubleElem(
                        Double.longBitsToDouble(reader.sizedLong(arg) << (8 * (7 - arg))), 'D',
                        name);
            case 0x17:
                return new AnnotationStringElem(strings[(int) reader.sizedLong(arg)], 's', name);
            case 0x18:
                return new AnnotationClassElem(findType((int) reader.sizedLong(arg)).toString(),
                        'c', name);
            case 0x19:
                int idx = (int) reader.sizedLong(arg);
                SootField field = fields[idx];
                RefType owner = field_owner[idx];
                return new AnnotationStringElem(owner.getClassName() + " "
                        + field.getSubSignature(), 'f', name);
            case 0x1a:
                idx = (int) reader.sizedLong(arg);
                SootMethod method = methods[idx];
                owner = method_owner[idx];
                return new AnnotationStringElem(owner.getClassName() + " "
                        + method.getSubSignature(), 'm', name);
            case 0x1b:
                idx = (int) reader.sizedLong(arg);
                SootField f = fields[idx];
                RefType t = field_owner[idx];
                return new AnnotationEnumElem(f.getName(), t.getClassName(), 'e', name);
            case 0x1c:
                int size = (int) reader.uleb128();
                ArrayList<AnnotationElem> elems = new ArrayList<AnnotationElem>();
                for (int i = 0; i < size; i++)
                    elems.add(parseAnnotationElem("default"));
                return new AnnotationArrayElem(elems, '[', name);
            case 0x1d:
                AnnotationTag annot = parseAnnotation();
                return new AnnotationAnnotationElem(annot, '@', name);
            case 0x1e:
                return new AnnotationIntElem(0, 'n', name);
            case 0x1f:
                return new AnnotationIntElem(arg, 'Z', name);
            default:
                throw new RuntimeException("Not a correct value_type tag");
        }
    }

    protected void buildMethodsCode() throws RefNotFoundException {
        int count = 0;
        for (Map.Entry<SootMethod, Integer> entry : builders.entrySet()) {
            MethodBuilder builder = new MethodBuilder(entry.getKey(), entry.getValue(), this);
            try {
                builder.makeBody();
                count++;
            } catch (RuntimeException e) {
                e.printStackTrace();
                D2JLog.warning("Failure on " + entry.getKey() + " after " + count + " / "
                        + builders.size() + " methods.");
                throw e;
            }
        }
    }

    /**
     * Parse the data segment defining the initial static values of a clas
     * 
     * @param offset
     * @return
     */
    private Constant[] parse_static_values(int offset) {
        if (offset == 0)
            return null;
        int save = reader.getPos();
        reader.seek(offset);
        int size = (int) reader.uleb128();
        Constant[] result = new Constant[size];
        for (int i = 0; i < size; i++) {
            result[i] = parseConstant();
            if (D2JLog.classes)
                D2JLog.info("Constant " + i + " : " + result[i]);
        }
        reader.seek(save);
        return result;
    }

    private Constant parseConstant() {
        int arg_type = reader.ubyte();
        int arg = (arg_type >> 5) & 0x7;
        switch (arg_type & 0x1f) {
            case 0:
                return IntConstant.v(reader.sbyte());
            case 2:
                return IntConstant.v(arg == 0 ? reader.sbyte() : reader.sshort());
            case 3:
                return IntConstant.v(arg == 0 ? reader.ubyte() : reader.ushort());
            case 4:
                return IntConstant.v((int) DalvikValueReader.completeSignSizedLong(
                        reader.sizedLong(arg), arg & 0x3));
            case 6:
                return LongConstant.v(DalvikValueReader.completeSignSizedLong(
                        reader.sizedLong(arg), arg));
            case 0x10:
                return FloatConstant
                        .v(Float.intBitsToFloat((int) reader.sizedLong(arg) << (8 * (3 - arg))));
            case 0x11:
                return DoubleConstant
                        .v(Double.longBitsToDouble(reader.sizedLong(arg) << (8 * (7 - arg))));
            case 0x17:
                return StringConstant.v(strings[(int) reader.sizedLong(arg)]);
            case 0x18:
                return ClassConstant.v(findType((int) reader.sizedLong(arg)).toString());
            case 0x19:
            case 0x1a:
            case 0x1b:
            case 0x1c:
            case 0x1d:
                throw new RuntimeException("Do not know how to handle those constant values");
            case 0x1e:
                return NullConstant.v();
            case 0x1f:
                return IntConstant.v(arg);
            default:
                throw new RuntimeException("Not a correct value_type tag");
        }
    }

    /**
     * Parse a class definition in the data segment.
     * 
     * @param clazz
     * @param classOff
     * @param staticValues
     */
    private void parseClass(SootClass clazz, int classOff, Constant[] staticValues) {
        if (classOff == 0)
            return;
        int saved = reader.getPos();
        reader.seek(classOff);
        int static_fields_size = (int) reader.uleb128();
        int instance_fields_size = (int) reader.uleb128();
        int direct_methods_size = (int) reader.uleb128();
        int virtual_methods_size = (int) reader.uleb128();
        int idx = 0;
        int flags, code_off;
        int stat_val_len = (staticValues == null) ? -1 : staticValues.length;
        for (int i = 0; i < static_fields_size; i++) {
            idx += reader.uleb128();
            SootField field = fields[idx];
            flags = (int) reader.uleb128();
            if (!clazz.declaresField(field.getName(), field.getType())) {
                clazz.addField(field);
                field.setModifiers(dalvikToJimple(flags));
                if (D2JLog.classes)
                    D2JLog.info("Static field " + field + " added.");
            } else {
                if (D2JLog.classes)
                    D2JLog.info("Static field " + field + " redefined.");
            }

            if (i < stat_val_len) {
                Tag tag = tagOfConstant(staticValues[i]);
                if (tag != null)
                    field.addTag(tag);
            }
        }

        idx = 0;
        for (int i = 0; i < instance_fields_size; i++) {
            idx += reader.uleb128();
            flags = (int) reader.uleb128();
            SootField field = fields[idx];
            if (!clazz.declaresField(field.getName(), field.getType())) {
                clazz.addField(field);
                field.setModifiers(dalvikToJimple(flags));
                if (D2JLog.classes)
                    D2JLog.info("Instance field " + field + " added.");
            } else {
                if (D2JLog.classes)
                    D2JLog.info("Instance field " + field + " redefined.");
            }

        }

        idx = 0;
        for (int i = 0; i < direct_methods_size; i++) {
            idx += reader.uleb128();
            flags = (int) reader.uleb128();
            code_off = (int) reader.uleb128();
            SootMethod meth = methods[idx];
            meth.setDeclaringClass(clazz);
            if (!clazz.declaresMethod(meth.getName(), meth.getParameterTypes())) {
                try {
                    clazz.addMethod(meth);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    D2JLog.warning("In clazz " + clazz + " problem for adding " + meth.getName()
                            + meth.getParameterTypes());
                    continue;
                }
            } else {
                if (D2JLog.classes)
                    D2JLog.info("Direct method " + meth + " redefined.");
                continue;
            }
            if (D2JLog.classes)
                D2JLog.info("Direct method " + meth + " added.");
            meth.setModifiers(dalvikToJimple(flags));
            if (code_off != 0) {
                builders.put(meth, code_off);
            }
        }

        idx = 0;
        for (int i = 0; i < virtual_methods_size; i++) {
            idx += reader.uleb128();
            flags = (int) reader.uleb128();
            code_off = (int) reader.uleb128();
            SootMethod meth = methods[idx];
            meth.setDeclaringClass(clazz);
            if (!clazz.declaresMethod(meth.getName(), meth.getParameterTypes(),
                    meth.getReturnType())) {
                try {
                    clazz.addMethod(meth);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    System.out.println(clazz.getMethods());
                    D2JLog.warning("In clazz " + clazz + " problem for adding virtual "
                            + meth.getName() + meth.getParameterTypes());
                    continue;
                }
            } else {
                if (D2JLog.classes)
                    D2JLog.info("Virtual method " + meth + " redefined.");
                continue;
            }
            if (D2JLog.classes)
                D2JLog.info("Instance method " + meth + " added.");
            meth.setModifiers(dalvikToJimple(flags));
            if (code_off != 0) {
                builders.put(meth, code_off);
            }
        }
        reader.seek(saved);
    }

    private Tag tagOfConstant(Constant constant) {
        if (constant instanceof NullConstant)
            return null;
        if (constant instanceof IntConstant)
            return new IntegerConstantValueTag(((IntConstant) constant).value);
        if (constant instanceof LongConstant)
            return new LongConstantValueTag(((LongConstant) constant).value);
        if (constant instanceof FloatConstant)
            return new FloatConstantValueTag(((FloatConstant) constant).value);
        if (constant instanceof DoubleConstant)
            return new DoubleConstantValueTag(((DoubleConstant) constant).value);
        if (constant instanceof StringConstant)
            return new StringConstantValueTag(((StringConstant) constant).value);
        return null;
    }

    /**
     * Parse flags from soot to jimple representation. It is the identity as
     * long as Java, Soot and Android have the same indexes.In fact Android use
     * some more flags in methods (ACC_BRIDGE, ACC_CONSTRUCTOR).
     * 
     * @param flags
     * @return
     */
    private int dalvikToJimple(int flags) {
        return flags;
    }

    /**
     * Static entry point.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        InputStream is;
        ZipFile apkFile = null;
        String filename = (args.length == 0) ? new File(System.getProperty("user.home"),
                "classes.dex").getAbsolutePath() : args[0];
        try {
            if (filename.endsWith(".apk")) {
                apkFile = new ZipFile(filename);
                ZipEntry classes = apkFile.getEntry("classes.dex");
                if (classes == null) {
                    throw new Exception("Cannot find classes in apk.");
                }
                is = apkFile.getInputStream(classes);
            } else
                is = new FileInputStream(filename);
            try {
                Scene.v().setSootClassPath(
                        "." + File.pathSeparator + new File(new File(System.getProperty("user.home"), "Android"), "android.jar") + File.pathSeparator);
                DexFile df = new DexFile();
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

    private void resolveTypes() {
        for (Type typ : types) {
            if (typ instanceof ArrayType)
                typ = ((ArrayType) typ).baseType;
            if (!(typ instanceof RefType))
                continue;
            RefType ref = (RefType) typ;
            if (ref.getSootClass().resolvingLevel() > 0)
                continue;
            Scene scene = Scene.v();
            System.err.println("Badly defined " + ref);
            String name = ref.getClassName();
            SootClass sc = scene.tryLoadClass(name, SootClass.SIGNATURES);
            if (sc == null) {
                System.err.println("Adding class");
                // The class may have been added at a wrong level
                SootClass fake = scene.getSootClass(name);
                if (fake != null)
                    scene.removeClass(fake);
                // Create a real class.
                sc = new SootClass(name);
                scene.addClass(sc);
            }
            ref.setSootClass(sc);
            unresolvedClasses.add(ref);
        }
    }

    /**
     * Find the type associated to an index. At this level, all classes are
     * known an resolved. If we must add a class then this class was not
     * correctly introduced.
     * 
     * @param index
     * @return
     */
    public Type findType(int index) {
        return types[index];
    }

}
