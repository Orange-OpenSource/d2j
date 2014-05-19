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

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.PatchingChain;
import soot.PrimType;
import soot.Printer;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Type;
import soot.Unit;
import soot.UnknownType;
import soot.Value;
import soot.ValueBox;
import soot.jimple.BinopExpr;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.Expr;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LongConstant;
import soot.jimple.MonitorStmt;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
import soot.jimple.internal.JAddExpr;
import soot.jimple.internal.JAndExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JCaughtExceptionRef;
import soot.jimple.internal.JCmpExpr;
import soot.jimple.internal.JCmpgExpr;
import soot.jimple.internal.JCmplExpr;
import soot.jimple.internal.JDivExpr;
import soot.jimple.internal.JEnterMonitorStmt;
import soot.jimple.internal.JEqExpr;
import soot.jimple.internal.JExitMonitorStmt;
import soot.jimple.internal.JGeExpr;
import soot.jimple.internal.JGotoStmt;
import soot.jimple.internal.JGtExpr;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JInstanceOfExpr;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JLeExpr;
import soot.jimple.internal.JLengthExpr;
import soot.jimple.internal.JLookupSwitchStmt;
import soot.jimple.internal.JLtExpr;
import soot.jimple.internal.JMulExpr;
import soot.jimple.internal.JNeExpr;
import soot.jimple.internal.JNegExpr;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNopStmt;
import soot.jimple.internal.JOrExpr;
import soot.jimple.internal.JRemExpr;
import soot.jimple.internal.JReturnStmt;
import soot.jimple.internal.JReturnVoidStmt;
import soot.jimple.internal.JShlExpr;
import soot.jimple.internal.JShrExpr;
import soot.jimple.internal.JSpecialInvokeExpr;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.internal.JSubExpr;
import soot.jimple.internal.JTableSwitchStmt;
import soot.jimple.internal.JThrowStmt;
import soot.jimple.internal.JTrap;
import soot.jimple.internal.JUshrExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JXorExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.typing.fast.TypeResolver;
import soot.tagkit.BytecodeOffsetTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.Chain;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

/**
 * Parse the code of a method body. It also infers types of locals and maintains
 * the list of traps.
 * 
 * @author piac6784
 */
public class MethodBuilder {

    final private SootClass throwableClass = Scene.v().loadClassAndSupport("java.lang.Throwable");

    /**
     * The method being built
     */
    private SootMethod method;

    /**
     * Code offset in the DEX file where to find the body definition.
     */
    private int codeOffset;

    /**
     * Handle to the current position in the DEX file (for parsing)
     */
    private DalvikValueReader reader;

    /**
     * Private pointer to the DEX file
     */
    private DexFile df;

    private int registers_size, ins_size, tries_size, instr_size, handlers_offset;

    @SuppressWarnings("unused")
    private int outs_size, debug_offset;

    /**
     * List of trap handlers for this body.
     */
    private List<JTrap> traps;

    /**
     * DEX instructions of the body (in ushort format).
     */
    private int[] instructions;

    /**
     * Locals declared by the body (non split).
     */
    private JimpleLocal[] locals;

    /**
     * Targets of jumps and branches. To be solved later.
     */
    private final List<TargetUnit> targets = new ArrayList<TargetUnit>();

    /**
     * Statement array for 1 to 1 correspondence with instructions.
     */
    private Stmt[] stmts;

    /**
     * This is a list of auxiliary locals to be added to the body. Mainly
     * generated for the compilation of <code>v = fill-array{ ... v ... }</code>
     */
    private List<Local> auxLocals;

    /**
     * This object will handle all the typing of the body.
     */
    private DvkTyper typer;

    /**
     * Constructor.
     * 
     * @param meth the method for which we build a body
     * @param codeOff the code offset to parse the body
     * @param df model of the Dex File. Contains the pools.
     */
    public MethodBuilder(SootMethod meth, int codeOff, DexFile df) {
        this.method = meth;
        this.codeOffset = codeOff;
        this.df = df;
        this.reader = df.reader;
        this.typer = new DvkTyper();
        auxLocals = new ArrayList<Local>();
    }

    /**
     * Build the Jimple body from the information stored in the dex file.
     * 
     * @throws RefNotFoundException
     * @throws UnsupportedEncodingException
     */
    public void makeBody() throws RefNotFoundException {
        parseBody();
        buildCode();
        JimpleBody body = linkBody();
        MyLocalSplitter.v().transform(body);
        // Here we only assign types to constants.
        typer.assignType();
        try {
            try {
                // Here we do the actual typing with the classical Soot type
                // inference algorithm
                TypeResolver fastResolver = new TypeResolver(body);
                fastResolver.inferTypes();
            } catch (RuntimeException e) {
                typeWithDecoupling(body);
            } catch (StackOverflowError e) {
                typeWithDecoupling(body);
            }
            if (D2JLog.print) {
                System.out.print("<" + method.getDeclaringClass() + ">");
                Printer.v().printTo(body,
                        new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true));
            }
            BodyCleaner cleaner = new BodyCleaner(body);

            cleaner.cleanUp();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not declared really", e);
        }
    }

    private void typeWithDecoupling(JimpleBody body) throws UnsupportedEncodingException {
        if (D2JLog.print) {
            System.out.println("INITIAL BODY");
            Printer.v().printTo(body,
                    new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true));
        }
        /*
         * This one may be thrown because of too tight constraints on variables.
         * We call the decoupler. Call again the spliter.
         */
        ConstantPropagator.transform(body);
        TypeDecoupler td = new TypeDecoupler(body);
        td.findLocals();
        MyLocalSplitter.v().transform(body);
        TypeResolver fastResolver = new TypeResolver(body);
        try {
            fastResolver.inferTypes();
        } catch (Exception e) {
            e.printStackTrace();
            debug(body);
        } catch (StackOverflowError e) {
            debug(body);
        }

    }

    /**
     * Auxiliary function for debugging typing problems.
     * <ul>
     * <li>First it prints the current body
     * <li>Then it dumps the CFG on a file.
     * <li>Finally it types the body with the OLD type checker. It is slow but
     * its output is more readable
     * </ul>
     * 
     * @param body
     */
    private void debug(JimpleBody body) {
        try {
            if (D2JLog.typing) {
                System.out.println("MODIFIED BODY");
                Printer.v().printTo(body,
                        new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"), true));
            }
            if (D2JLog.dot) {
                ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
                CFGToDotGraph cdg = new CFGToDotGraph();
                DotGraph dg = cdg.drawCFG(graph);
                dg.plot("out.dot");
            }
            soot.jimple.toolkits.typing.TypeResolver.resolve(body, Scene.v());
        } catch (Exception e) {
            System.err.println("Error while outputing body");
            e.printStackTrace();
        }
    }

    private Type findType(int index) {
        return df.findType(index);
    }

    private void parseBody() {
        int save = reader.getPos();
        reader.seek(codeOffset);
        registers_size = reader.ushort();
        ins_size = reader.ushort();
        outs_size = reader.ushort();
        tries_size = reader.ushort();
        debug_offset = (int) reader.uint();
        instr_size = (int) reader.uint();
        instructions = new int[instr_size];

        for (int i = 0; i < instr_size; i++)
            instructions[i] = reader.ushort();
        if (instr_size % 2 != 0)
            reader.ushort();
        handlers_offset = reader.getPos() + 8 * tries_size;
        int start, count, off;
        traps = new ArrayList<JTrap>();
        for (int i = 0; i < tries_size; i++) {
            start = (int) reader.uint();
            count = reader.ushort();
            off = reader.ushort();
            buildTraps(start, count, off);
        }
        reader.seek(save);
    }

    private void buildCode() throws RefNotFoundException {
        List<Value> argList;
        InvokeExpr invokeExpr;
        Expr expr;
        Value arg1, arg2;
        int arg_size, opcode;
        locals = new JimpleLocal[registers_size];
        for (int i = 0; i < registers_size; i++)
            locals[i] = new JimpleLocal("R" + i, UnknownType.v());
        int pc = 0;
        stmts = new Stmt[instr_size];
        if (D2JLog.instructions)
            D2JLog.info("Starting code for method " + method);
        while (pc < instr_size) {
            int debug_pc = pc;
            opcode = instructions[pc] & 0xFF;
            // System.err.println(pc + " : " + Integer.toHexString(current));
            switch (opcode) {
                case 0x00:
                    switch (aa(pc)) {
                        case 0: // Regular NOP
                            stmts[pc] = new JNopStmt();
                            pc++;
                            break;
                        case 1: // packed switch
                            pc += c(pc) * 2 + 4;
                            break;
                        case 2: // sparse switch
                            pc += c(pc) * 4 + 2;
                            break;
                        case 3: // array data
                            pc += (c(pc) * dI(pc) + 1) / 2 + 4;
                            break;
                        default:
                            D2JLog.info("Unhandled case for NOP instruction " + aa(pc));
                    }   
                    break;
                case 0x01:
                case 0x04:
                case 0x07:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)], locals[b(pc)]), opcode);
                    pc++;
                    break;
                case 0x02:
                case 0x05:
                case 0x08:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[aa(pc)], locals[c(pc)]),
                            opcode);
                    pc += 2;
                    break;
                case 0x03:
                case 0x06:
                case 0x09:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[c(pc)], locals[d(pc)]), opcode);
                    pc += 3;
                    break;
                case 0x0a:
                case 0x0b:
                case 0x0c:
                    throw new RuntimeException("To be treated by invoke");
                case 0x0d:
                    stmts[pc] = new JIdentityStmt(locals[aa(pc)], new JCaughtExceptionRef());
                    pc++;
                    break;
                case 0x0e:
                    stmts[pc] = new JReturnVoidStmt();
                    pc++;
                    break;
                case 0x0f:
                case 0x10:
                case 0x11:
                    JReturnStmt retStmt = new JReturnStmt(locals[aa(pc)]);
                    ValueBox box = retStmt.getOpBox();
                    typer.setType(box, method.getReturnType());
                    stmts[pc] = retStmt;
                    pc++;
                    break;
                case 0x12:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)], IntConstant.v(b(pc))),
                            opcode);
                    pc++;
                    break;
                case 0x13:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], IntConstant.v(c(pc))), opcode);
                    pc += 2;
                    break;
                case 0x14:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], IntConstant.v(cI(pc))), opcode);
                    pc += 3;
                    break;
                case 0x15:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], IntConstant.v(c(pc) << 16)), opcode);
                    pc += 2;
                    break;
                case 0x16:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], LongConstant.v(c(pc))), opcode);
                    pc += 2;
                    break;
                case 0x17:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], LongConstant.v(cI(pc))), opcode);
                    pc += 3;
                    break;
                case 0x18:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], LongConstant.v(cL(pc))), opcode);
                    pc += 5;
                    break;
                case 0x19:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], LongConstant.v((long) c(pc) << 48)),
                            opcode);
                    pc += 2;
                    break;
                case 0x1a:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], StringConstant.v(df.strings[c(pc)])),
                            opcode);
                    pc += 2;
                    break;
                case 0x1b:
                    stmts[pc] = captureAssign(
                            new JAssignStmt(locals[aa(pc)], StringConstant.v(df.strings[cI(pc)])),
                            opcode);
                    pc += 3;
                    break;
                case 0x1c:
                    Type classtype = findType(c(pc));
                    if (classtype != null) {
                        String classname = (classtype instanceof RefType) ? ((RefType) classtype)
                                .getClassName() : ((ArrayType) classtype).toString(); // TODO handling of arrays
                        // TODO ugly change from 2.3 to 2.5 $ to be put back somewhere.
                        stmts[pc] = captureAssign(
                                new JAssignStmt(locals[aa(pc)], ClassConstant.v(classname.replace('.', '/'))), opcode);
                    } else {
                        stmts[pc] = new JAssignStmt(locals[aa(pc)], NullConstant.v());
                        // new JNopStmt();
                    }
                    pc += 2;
                    break;
                case 0x1d:
                    stmts[pc] = captureMonitor(new JEnterMonitorStmt(locals[aa(pc)]));
                    pc++;
                    break;
                case 0x1e:
                    stmts[pc] = captureMonitor(new JExitMonitorStmt(locals[aa(pc)]));
                    pc++;
                    break;
                case 0x1f:
                    Type typ = findType(c(pc));
                    if (typ != null) {
                        stmts[pc] = captureAssign(new JAssignStmt(locals[aa(pc)], new JCastExpr(
                                locals[aa(pc)], typ)), opcode);
                    } else {
                        // Kind of JNop, reset lhs
                        stmts[pc] = new JAssignStmt(locals[aa(pc)], NullConstant.v());
                    }
                    pc += 2;
                    break;
                case 0x20:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)], new JInstanceOfExpr(
                            locals[b(pc)], findType(c(pc)))), opcode);

                    pc += 2;
                    break;
                case 0x21:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)], new JLengthExpr(
                            locals[b(pc)])), opcode);
                    pc++;
                    break;
                case 0x22:
                    RefType refType = (RefType) findType(c(pc));
                    if (refType != null) {
                        stmts[pc] = captureAssign(new JAssignStmt(locals[aa(pc)], new JNewExpr(
                                refType)), opcode);
                    } else {
                        stmts[pc] = new JAssignStmt(locals[aa(pc)], NullConstant.v());
                    }
                    pc += 2;
                    break;
                case 0x23:
                    ArrayType atype = (ArrayType) findType(c(pc));
                    if (atype != null) {
                        JNewArrayExpr newArrayExpr = new JNewArrayExpr(atype.getArrayElementType(),
                                locals[b(pc)]);
                        typer.setType(newArrayExpr.getSizeBox(), IntType.v());
                        stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)], newArrayExpr),
                                opcode);
                    } else {
                        stmts[pc] = new JAssignStmt(locals[a(pc)], NullConstant.v());
                        // stmts[pc] = new JNopStmt();
                    }
                    pc += 2;
                    break;
                case 0x24:
                    atype = (ArrayType) findType(c(pc));
                    if (atype != null) {
                        argList = new ArrayList<Value>();
                        arg_size = b(pc);
                        if (arg_size > 0)
                            argList.add(locals[d1(pc)]);
                        if (arg_size > 1)
                            argList.add(locals[d2(pc)]);
                        if (arg_size > 2)
                            argList.add(locals[d3(pc)]);
                        if (arg_size > 3)
                            argList.add(locals[d4(pc)]);
                        if (arg_size > 4)
                            argList.add(locals[a(pc)]);
                        pc += 3;
                        switch (instructions[pc] & 0xff) {
                            case 0x0a:
                            case 0x0b:
                            case 0x0c:
                                stmts[pc - 3] = new FillArrayStmt(locals[aa(pc)], argList, atype);
                                pc++;
                                break;
                            default:
                                throw new RuntimeException("Not a correct fill-new-array 1"
                                        + instructions[pc] + " at " + method + ":" + pc);
                        }
                    } else {
                        stmts[pc] = new JAssignStmt(locals[aa(pc)], NullConstant.v());
                        pc += 4;
                    }
                    break;
                case 0x25:
                    atype = (ArrayType) findType(c(pc));
                    if (atype != null) {
                        argList = new ArrayList<Value>();
                        arg_size = aa(pc);
                        int base_arg = d(pc);
                        for (int i = 0; i < arg_size; i++)
                            argList.add(locals[base_arg + i]);
                        pc += 3;
                        switch (instructions[pc]) {
                            case 0x0a:
                            case 0x0b:
                            case 0x0c:
                                stmts[pc - 3] = new FillArrayStmt(locals[aa(pc)], argList, atype);
                                pc++;
                                break;
                            default:
                                throw new RuntimeException("Not a correct fill-new-array 2");
                        }
                    } else {
                        stmts[pc] = new JAssignStmt(locals[aa(pc)], NullConstant.v());
                        pc += 4;
                    }
                    break;
                case 0x26:
                    argList = readArrayData(pc + cI(pc));
                    stmts[pc] = new FillArrayStmt(locals[aa(pc)], argList, null);
                    pc += 3;
                    break;
                case 0x27:
                    JThrowStmt throwStmt = new JThrowStmt(locals[aa(pc)]);
                    stmts[pc] = throwStmt;
                    typer.setObjectType(throwStmt.getOpBox());
                    pc++;
                    break;
                case 0x28:
                    stmts[pc] = new JGotoStmt(targetUnit(pc + (byte) aa(pc)));
                    pc++;
                    break;
                case 0x29:
                    stmts[pc] = new JGotoStmt(targetUnit(pc + (short) c(pc)));
                    pc += 2;
                    break;
                case 0x2a:
                    stmts[pc] = new JGotoStmt(targetUnit(pc + cI(pc)));
                    pc += 2;
                    break;
                case 0x2b:
                    stmts[pc] = parsePackedStmt(locals[aa(pc)], pc, pc + cI(pc));
                    pc += 3;
                    break;
                case 0x2c:
                    stmts[pc] = parseSparseStmt(locals[aa(pc)], pc, pc + cI(pc));
                    pc += 3;
                    break;
                case 0x2d:
                case 0x2e:
                case 0x2f:
                case 0x30:
                case 0x31:
                    arg1 = locals[c1(pc)];
                    arg2 = locals[c2(pc)];
                    switch (opcode) {
                        case 0x2d:
                        case 0x2f:
                            expr = new JCmplExpr(arg1, arg2);
                            Type type = (opcode == 0x2d) ? FloatType.v() : DoubleType.v();
                            typer.setType(((BinopExpr) expr).getOp1Box(), type);
                            typer.setType(((BinopExpr) expr).getOp2Box(), type);
                            break;
                        case 0x2e:
                        case 0x30:
                            expr = new JCmpgExpr(arg1, arg2);
                            type = (opcode == 0x2e) ? FloatType.v() : DoubleType.v();
                            typer.setType(((BinopExpr) expr).getOp1Box(), type);
                            typer.setType(((BinopExpr) expr).getOp2Box(), type);
                            break;
                        case 0x31:
                            expr = new JCmpExpr(arg1, arg2);
                            typer.setType(((BinopExpr) expr).getOp1Box(), LongType.v());
                            typer.setType(((BinopExpr) expr).getOp2Box(), LongType.v());
                            break;
                        default:
                            expr = null;
                            assert false;
                    }
                    stmts[pc] = new JAssignStmt(locals[aa(pc)], expr);
                    pc += 2;
                    break;
                case 0x32:
                case 0x33:
                case 0x34:
                case 0x35:
                case 0x36:
                case 0x37:
                    arg1 = locals[a(pc)];
                    arg2 = locals[b(pc)];
                    switch (opcode) {
                        case 0x32:
                            expr = new JEqExpr(arg1, arg2);
                            break;
                        case 0x33:
                            expr = new JNeExpr(arg1, arg2);
                            break;
                        case 0x34:
                            expr = new JLtExpr(arg1, arg2);
                            break;
                        case 0x35:
                            expr = new JGeExpr(arg1, arg2);
                            break;
                        case 0x36:
                            expr = new JGtExpr(arg1, arg2);
                            break;
                        case 0x37:
                            expr = new JLeExpr(arg1, arg2);
                            break;
                        default:
                            expr = null;
                            assert false;
                    }
                    typer.setConstraint(((BinopExpr) expr).getOp1Box(),
                            ((BinopExpr) expr).getOp2Box());
                    stmts[pc] = new JIfStmt(expr, targetUnit(pc + (short) c(pc)));
                    pc += 2;
                    break;
                case 0x38:
                case 0x39:
                case 0x3a:
                case 0x3b:
                case 0x3c:
                case 0x3d:
                    arg1 = locals[aa(pc)];
                    arg2 = IntConstant.v(0);
                    switch (opcode) {
                        case 0x38: // Warning may be a test to null
                            expr = new JEqExpr(arg1, arg2);
                            typer.setConstraint(((BinopExpr) expr).getOp1Box(),
                                    ((BinopExpr) expr).getOp2Box());
                            break;
                        case 0x39: // Warning may be a test to non null
                            expr = new JNeExpr(arg1, arg2);
                            typer.setConstraint(((BinopExpr) expr).getOp1Box(),
                                    ((BinopExpr) expr).getOp2Box());
                            break;
                        case 0x3a:
                            expr = new JLtExpr(arg1, arg2);
                            typer.setType(((BinopExpr) expr).getOp1Box(), IntType.v());
                            break;
                        case 0x3b:
                            expr = new JGeExpr(arg1, arg2);
                            typer.setType(((BinopExpr) expr).getOp1Box(), IntType.v());
                            break;
                        case 0x3c:
                            expr = new JGtExpr(arg1, arg2);
                            typer.setType(((BinopExpr) expr).getOp1Box(), IntType.v());
                            break;
                        case 0x3d:
                            expr = new JLeExpr(arg1, arg2);
                            typer.setType(((BinopExpr) expr).getOp1Box(), IntType.v());
                            break;
                        default:
                            expr = null;
                            assert false;
                    }

                    stmts[pc] = new JIfStmt(expr, targetUnit(pc + (short) c(pc)));
                    pc += 2;
                    break;
                case 0x44:
                case 0x45:
                case 0x46:
                case 0x47:
                case 0x48:
                case 0x49:
                case 0x4a:
                    stmts[pc] = captureAssign(new JAssignStmt(locals[aa(pc)], new JArrayRef(
                            locals[c1(pc)], locals[c2(pc)])), opcode);
                    pc += 2;
                    break;
                case 0x4b:
                case 0x4c:
                case 0x4d:
                case 0x4e:
                case 0x4f:
                case 0x50:
                case 0x51:
                    stmts[pc] = captureAssign(new JAssignStmt(new JArrayRef(locals[c1(pc)],
                            locals[c2(pc)]), locals[aa(pc)]), opcode);
                    pc += 2;
                    break;
                case 0x52:
                case 0x53:
                case 0x54:
                case 0x55:
                case 0x56:
                case 0x57:
                case 0x58:
                    SootFieldRef sfr = df.getFieldRef(c(pc), false);
                    try {
                        sfr.resolve();
                        stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)],
                                new JInstanceFieldRef(locals[b(pc)], sfr)), opcode);
                    } catch (RuntimeException e) {
                        df.unresolvedFields.add(sfr);
                        stmts[pc] = captureAssign(new JAssignStmt(locals[a(pc)], IntConstant.v(0)),
                                0x12);
                    }
                    pc += 2;
                    break;
                case 0x59:
                case 0x5a:
                case 0x5b:
                case 0x5c:
                case 0x5d:
                case 0x5e:
                case 0x5f:
                    sfr = df.getFieldRef(c(pc), false);
                    try {
                        sfr.resolve();
                        stmts[pc] = captureAssign(new JAssignStmt(new JInstanceFieldRef(
                                locals[b(pc)], sfr), locals[a(pc)]), opcode);
                    } catch (RuntimeException e) {
                        df.unresolvedFields.add(sfr);
                        stmts[pc] = new JNopStmt();
                    }
                    pc += 2;
                    break;

                case 0x60:
                case 0x61:
                case 0x62:
                case 0x63:
                case 0x64:
                case 0x65:
                case 0x66:
                    sfr = df.getFieldRef(c(pc), true);
                    try {
                        sfr.resolve();
                        stmts[pc] = captureAssign(new JAssignStmt(locals[aa(pc)], Jimple.v()
                                .newStaticFieldRef(sfr)), opcode);
                    } catch (RuntimeException e) {
                        df.unresolvedFields.add(sfr);
                        stmts[pc] = captureAssign(
                                new JAssignStmt(locals[aa(pc)], IntConstant.v(0)), 0x12);
                    }
                    pc += 2;
                    break;
                case 0x67:
                case 0x68:
                case 0x69:
                case 0x6a:
                case 0x6b:
                case 0x6c:
                case 0x6d:
                    sfr = df.getFieldRef(c(pc), true);
                    try {
                        sfr.resolve();
                        stmts[pc] = captureAssign(
                                new JAssignStmt(Jimple.v().newStaticFieldRef(
                                        df.getFieldRef(c(pc), true)), locals[aa(pc)]), opcode);
                    } catch (RuntimeException e) {
                        df.unresolvedFields.add(sfr);
                        stmts[pc] = new JNopStmt();
                    }
                    pc += 2;
                    break;
                case 0x6e:
                case 0x6f:
                case 0x70:
                case 0x71:
                case 0x72:
                    argList = new ArrayList<Value>();
                    arg_size = b(pc);
                    if (arg_size > 0)
                        argList.add(locals[d1(pc)]);
                    if (arg_size > 1)
                        argList.add(locals[d2(pc)]);
                    if (arg_size > 2)
                        argList.add(locals[d3(pc)]);
                    if (arg_size > 3)
                        argList.add(locals[d4(pc)]);
                    if (arg_size > 4)
                        argList.add(locals[a(pc)]);
                    SootMethodRef methodRef = df.getMethodRef(c(pc), opcode == 0x71);
                    invokeExpr = buildInvokeExpr(methodRef, argList, opcode - 0x6e);
                    if (invokeExpr == null)
                        pc = emitNoInvoke(stmts, pc, methodRef.returnType());
                    else
                        pc = emitInvoke(stmts, invokeExpr, pc);
                    break;
                case 0x74:
                case 0x75:
                case 0x76:
                case 0x77:
                case 0x78:
                    argList = new ArrayList<Value>();
                    arg_size = aa(pc);
                    int base_arg = d(pc);
                    for (int i = 0; i < arg_size; i++)
                        argList.add(locals[base_arg + i]);
                    methodRef = df.getMethodRef(c(pc), opcode == 0x77);
                    invokeExpr = buildInvokeExpr(methodRef, argList, opcode - 0x74);
                    if (invokeExpr == null)
                        pc = emitNoInvoke(stmts, pc, methodRef.returnType());
                    else
                        pc = emitInvoke(stmts, invokeExpr, pc);
                    break;
                case 0x7b:
                case 0x7c:
                case 0x7d:
                case 0x7e:
                case 0x7f:
                case 0x80:
                case 0x81:
                case 0x82:
                case 0x83:
                case 0x84:
                case 0x85:
                case 0x86:
                case 0x87:
                case 0x88:
                case 0x89:
                case 0x8a:
                case 0x8b:
                case 0x8c:
                case 0x8d:
                case 0x8e:
                case 0x8f:
                    expr = unop(opcode, locals[b(pc)]);
                    JAssignStmt assignStmt = new JAssignStmt(locals[a(pc)], expr);
                    stmts[pc] = assignStmt;
                    typer.setType(assignStmt.leftBox, resUnType[opcode - 0x7b]);
                    pc++;
                    break;
                case 0x90:
                case 0x91:
                case 0x92:
                case 0x93:
                case 0x94:
                case 0x95:
                case 0x96:
                case 0x97:
                case 0x98:
                case 0x99:
                case 0x9a:
                case 0x9b:
                case 0x9c:
                case 0x9d:
                case 0x9e:
                case 0x9f:
                case 0xa0:
                case 0xa1:
                case 0xa2:
                case 0xa3:
                case 0xa4:
                case 0xa5:
                case 0xa6:
                case 0xa7:
                case 0xa8:
                case 0xa9:
                case 0xaa:
                case 0xab:
                case 0xac:
                case 0xad:
                case 0xae:
                case 0xaf:
                    arg1 = locals[c1(pc)];
                    arg2 = locals[c2(pc)];
                    expr = binop(opcode - 0x90, arg1, arg2);
                    assignStmt = new JAssignStmt(locals[aa(pc)], expr);
                    stmts[pc] = assignStmt;
                    typer.setType(assignStmt.leftBox, resBinType[opcode - 0x90]);
                    pc += 2;
                    break;
                case 0xb0:
                case 0xb1:
                case 0xb2:
                case 0xb3:
                case 0xb4:
                case 0xb5:
                case 0xb6:
                case 0xb7:
                case 0xb8:
                case 0xb9:
                case 0xba:
                case 0xbb:
                case 0xbc:
                case 0xbd:
                case 0xbe:
                case 0xbf:
                case 0xc0:
                case 0xc1:
                case 0xc2:
                case 0xc3:
                case 0xc4:
                case 0xc5:
                case 0xc6:
                case 0xc7:
                case 0xc8:
                case 0xc9:
                case 0xca:
                case 0xcb:
                case 0xcc:
                case 0xcd:
                case 0xce:
                case 0xcf:
                    arg1 = locals[a(pc)];
                    arg2 = locals[b(pc)];
                    expr = binop(opcode - 0xb0, arg1, arg2);
                    assignStmt = new JAssignStmt(locals[a(pc)], expr);
                    stmts[pc] = assignStmt;
                    typer.setType(assignStmt.leftBox, resBinType[opcode - 0xb0]);
                    pc++;
                    break;

                case 0xd0:
                case 0xd1:
                case 0xd2:
                case 0xd3:
                case 0xd4:
                case 0xd5:
                case 0xd6:
                case 0xd7:
                    arg1 = locals[b(pc)];
                    arg2 = IntConstant.v(c(pc));
                    expr = binop2(opcode - 0xd0, arg1, arg2);
                    stmts[pc] = new JAssignStmt(locals[a(pc)], expr);
                    pc += 2;
                    break;
                case 0xd8:
                case 0xd9:
                case 0xda:
                case 0xdb:
                case 0xdc:
                case 0xdd:
                case 0xde:
                case 0xdf:
                case 0xe0:
                case 0xe1:
                case 0xe2:
                    arg1 = locals[c1(pc)];
                    arg2 = IntConstant.v(c2(pc));
                    expr = binop2(opcode - 0xd8, arg1, arg2);
                    stmts[pc] = new JAssignStmt(locals[aa(pc)], expr);
                    pc += 2;
                    break;
                default:
                    throw new RuntimeException("Unknown instruction " + opcode + " at " + pc);
            }
            if (D2JLog.instructions)
                D2JLog.info(Integer.toHexString(debug_pc) + ":\t" + stmts[debug_pc] + "\t\t( -> "
                        + pc + ")");
        }
    }

    private Stmt captureMonitor(MonitorStmt stmt) {
        return stmt;
    }

    private Stmt captureAssign(JAssignStmt stmt, int current) {
        ValueBox left = stmt.leftBox;
        ValueBox right = stmt.rightBox;
        switch (current) {
            case 0x01:
            case 0x02:
            case 0x03:
            case 0x04:
            case 0x05:
            case 0x06:
            case 0x12:
            case 0x13:
            case 0x14:
            case 0x15:
            case 0x16:
            case 0x17:
            case 0x18:
            case 0x19:
            case 0x4b:
            case 0x4c:
            case 0x4d:
            case 0x4e:
            case 0x4f:
            case 0x50:
            case 0x51:
                typer.setConstraint(left, right);
                break;
            case 0x07:
            case 0x08:
            case 0x09:
                typer.setObjectType(right);
                typer.setConstraint(left, right);
                break;
            case 0x1a:
            case 0x1b:
                typer.setType(left, Scene.v().getRefType("java.lang.String"));
                break;
            case 0x1c:
                typer.setType(left, Scene.v().getRefType("java.lang.Class"));
                break;
            case 0x1f:
                typer.setType(left, right.getValue().getType());
                typer.setType(((JCastExpr) right.getValue()).getOpBox(), right.getValue().getType());
                break;
            case 0x20:
                typer.setType(left, BooleanType.v());
                typer.setObjectType(((JInstanceOfExpr) right.getValue()).getOpBox());
                break;
            case 0x21:
                typer.setType(left, IntType.v());
                break;

            case 0x44:
            case 0x45:
            case 0x46:
            case 0x47:
            case 0x48:
            case 0x49:
            case 0x4a:
                Value arrayGetValue = right.getValue();
                if (arrayGetValue instanceof JArrayRef) {
                    typer.setObjectType(((JArrayRef) arrayGetValue).getBaseBox());
                }
                typer.setConstraint(right, left);
                break;

            case 0x52:
            case 0x53:
            case 0x54:
            case 0x55:
            case 0x56:
            case 0x57:
            case 0x58:
                Value fieldGetValue = right.getValue();
                if (fieldGetValue instanceof JInstanceFieldRef) {
                    typer.setObjectType(((JInstanceFieldRef) fieldGetValue).getBaseBox());
                }
                typer.setType(left, right.getValue().getType());
                break;
            case 0xa:
            case 0x22:
            case 0x23:
            case 0x24:
            case 0x60:
            case 0x61:
            case 0x62:
            case 0x63:
            case 0x64:
            case 0x65:
            case 0x66:
                typer.setType(left, right.getValue().getType());
                break;
            case 0x59:
            case 0x5a:
            case 0x5b:
            case 0x5c:
            case 0x5d:
            case 0x5e:
            case 0x5f:
            case 0x67:
            case 0x68:
            case 0x69:
            case 0x6a:
            case 0x6b:
            case 0x6c:
            case 0x6d:
                typer.setType(right, left.getValue().getType());
                break;
            default:
                D2JLog.warning("No constraint registered for opcode " + current);
        }
        return stmt;

    }

    private Type[] opUnType = {
            IntType.v(), IntType.v(), LongType.v(), LongType.v(), FloatType.v(), DoubleType.v(),
            IntType.v(), IntType.v(), IntType.v(), LongType.v(), LongType.v(), LongType.v(),
            FloatType.v(), FloatType.v(), FloatType.v(), DoubleType.v(), DoubleType.v(),
            DoubleType.v(), IntType.v(), IntType.v(), IntType.v()
    };

    private Type[] resUnType = {
            IntType.v(), IntType.v(), LongType.v(), LongType.v(), FloatType.v(), DoubleType.v(),
            LongType.v(), FloatType.v(), DoubleType.v(), IntType.v(), FloatType.v(),
            DoubleType.v(), IntType.v(), LongType.v(), DoubleType.v(), IntType.v(), LongType.v(),
            FloatType.v(), IntType.v(), IntType.v(), IntType.v()
    };

    private Expr unop(int op, Value arg) {
        Expr expr;
        switch (op) {
            case 0x7b:
                expr = new JNegExpr(arg);
                break;
            case 0x7c:
                throw new RuntimeException("Who generates the Not ?");
                // expr = new JNotExpr(arg);
            case 0x7d:
                expr = new JNegExpr(arg);
                break;
            case 0x7e:
                throw new RuntimeException("Who generates the Not ?");
                // expr = new JNotExpr(arg);
            case 0x7f:
                expr = new JNegExpr(arg);
                break;
            case 0x80:
                expr = new JNegExpr(arg);
                break;
            case 0x81:
                expr = new JCastExpr(arg, LongType.v());
                break;
            case 0x82:
                expr = new JCastExpr(arg, FloatType.v());
                break;
            case 0x83:
                expr = new JCastExpr(arg, DoubleType.v());
                break;
            case 0x84:
                expr = new JCastExpr(arg, IntType.v());
                break;
            case 0x85:
                expr = new JCastExpr(arg, FloatType.v());
                break;
            case 0x86:
                expr = new JCastExpr(arg, DoubleType.v());
                break;
            case 0x87:
                expr = new JCastExpr(arg, IntType.v());
                break;
            case 0x88:
                expr = new JCastExpr(arg, LongType.v());
                break;
            case 0x89:
                expr = new JCastExpr(arg, DoubleType.v());
                break;
            case 0x8a:
                expr = new JCastExpr(arg, IntType.v());
                break;
            case 0x8b:
                expr = new JCastExpr(arg, LongType.v());
                break;
            case 0x8c:
                expr = new JCastExpr(arg, FloatType.v());
                break;
            case 0x8d:
                expr = new JCastExpr(arg, ByteType.v());
                break;
            case 0x8e:
                expr = new JCastExpr(arg, CharType.v());
                break;
            case 0x8f:
                expr = new JCastExpr(arg, ShortType.v());
                break;
            default:
                expr=null;
        }
        if (expr != null) {
            typer.setType((expr instanceof JCastExpr) ? ((JCastExpr) expr).getOpBox()
                    : ((UnopExpr) expr).getOpBox(), opUnType[op - 0x7b]);
        }
        return expr;
    }

    private Type[] resBinType = {
            IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(),
            IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(), LongType.v(),
            LongType.v(), LongType.v(), LongType.v(), LongType.v(), LongType.v(), LongType.v(),
            LongType.v(), LongType.v(), LongType.v(), LongType.v(), FloatType.v(), FloatType.v(),
            FloatType.v(), FloatType.v(), FloatType.v(), DoubleType.v(), DoubleType.v(),
            DoubleType.v(), DoubleType.v(), DoubleType.v()
    };

    private Type[] op1BinType = {
            IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(),
            IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(), LongType.v(),
            LongType.v(), LongType.v(), LongType.v(), LongType.v(), LongType.v(), LongType.v(),
            LongType.v(), LongType.v(), LongType.v(), LongType.v(), FloatType.v(), FloatType.v(),
            FloatType.v(), FloatType.v(), FloatType.v(), DoubleType.v(), DoubleType.v(),
            DoubleType.v(), DoubleType.v(), DoubleType.v()
    };

    private Type[] op2BinType = {
            IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(),
            IntType.v(), IntType.v(), IntType.v(), IntType.v(), IntType.v(), LongType.v(),
            LongType.v(), LongType.v(), LongType.v(), LongType.v(), LongType.v(), LongType.v(),
            LongType.v(), IntType.v(), IntType.v(), IntType.v(), FloatType.v(), FloatType.v(),
            FloatType.v(), FloatType.v(), FloatType.v(), DoubleType.v(), DoubleType.v(),
            DoubleType.v(), DoubleType.v(), DoubleType.v()
    };

    private Expr binop(int op, Value arg1, Value arg2) {
        BinopExpr bop;
        switch (op) {
            case 0x0:
                bop = new JAddExpr(arg1, arg2);
                break;
            case 0x1:
                bop = new JSubExpr(arg1, arg2);
                break;
            case 0x2:
                bop = new JMulExpr(arg1, arg2);
                break;
            case 0x3:
                bop = new JDivExpr(arg1, arg2);
                break;
            case 0x4:
                bop = new JRemExpr(arg1, arg2);
                break;
            case 0x5:
                bop = new JAndExpr(arg1, arg2);
                break;
            case 0x6:
                bop = new JOrExpr(arg1, arg2);
                break;
            case 0x7:
                bop = new JXorExpr(arg1, arg2);
                break;
            case 0x8:
                bop = new JShlExpr(arg1, arg2);
                break;
            case 0x9:
                bop = new JShrExpr(arg1, arg2);
                break;
            case 0xa:
                bop = new JUshrExpr(arg1, arg2);
                break;
            case 0xb:
                bop = new JAddExpr(arg1, arg2);
                break;
            case 0xc:
                bop = new JSubExpr(arg1, arg2);
                break;
            case 0xd:
                bop = new JMulExpr(arg1, arg2);
                break;
            case 0xe:
                bop = new JDivExpr(arg1, arg2);
                break;
            case 0xf:
                bop = new JRemExpr(arg1, arg2);
                break;
            case 0x10:
                bop = new JAndExpr(arg1, arg2);
                break;
            case 0x11:
                bop = new JOrExpr(arg1, arg2);
                break;
            case 0x12:
                bop = new JXorExpr(arg1, arg2);
                break;
            case 0x13:
                bop = new JShlExpr(arg1, arg2);
                break;
            case 0x14:
                bop = new JShrExpr(arg1, arg2);
                break;
            case 0x15:
                bop = new JUshrExpr(arg1, arg2);
                break;
            case 0x16:
                bop = new JAddExpr(arg1, arg2);
                break;
            case 0x17:
                bop = new JSubExpr(arg1, arg2);
                break;
            case 0x18:
                bop = new JMulExpr(arg1, arg2);
                break;
            case 0x19:
                bop = new JDivExpr(arg1, arg2);
                break;
            case 0x1a:
                bop = new JRemExpr(arg1, arg2);
                break;
            case 0x1b:
                bop = new JAddExpr(arg1, arg2);
                break;
            case 0x1c:
                bop = new JSubExpr(arg1, arg2);
                break;
            case 0x1d:
                bop = new JMulExpr(arg1, arg2);
                break;
            case 0x1e:
                bop = new JDivExpr(arg1, arg2);
                break;
            case 0x1f:
                bop = new JRemExpr(arg1, arg2);
                break;
            default:
                bop = null;
        }
        if (bop != null) {
            typer.setType(bop.getOp1Box(), op1BinType[op]);
            typer.setType(bop.getOp2Box(), op2BinType[op]);
        }
        return bop;
    }

    private Expr binop2(int op, Value arg1, Value arg2) {
        BinopExpr bop;
        switch (op) {
            case 0x0:
                bop = new JAddExpr(arg1, arg2);
                break;
            case 0x1:
                bop = new JSubExpr(arg2, arg1); // Beware special case !
                break;
            case 0x2:
                bop = new JMulExpr(arg1, arg2);
                break;
            case 0x3:
                bop = new JDivExpr(arg1, arg2);
                break;
            case 0x4:
                bop = new JRemExpr(arg1, arg2);
                break;
            case 0x5:
                bop = new JAndExpr(arg1, arg2);
                break;
            case 0x6:
                bop = new JOrExpr(arg1, arg2);
                break;
            case 0x7:
                bop = new JXorExpr(arg1, arg2);
                break;
            case 0x8:
                bop = new JShlExpr(arg1, arg2);
                break;
            case 0x9:
                bop = new JShrExpr(arg1, arg2);
                break;
            case 0xa:
                bop = new JUshrExpr(arg1, arg2);
                break;
            default:
                bop = null;
        }
        if (bop != null)
            typer.setType((op == 1) ? bop.getOp2Box() : bop.getOp1Box(), op1BinType[op]);
        return bop;
    }

    private List<Value> readArrayData(int pc) {
        int width = c(pc);
        int size = dI(pc);
        pc += 4;
        List<Value> table = new ArrayList<Value>();
        for (int i = 0; i < size; i++)
            table.add(constant(pc, i, width));
        return table;
    }

    private Value constant(int pc, int i, int width) {
        switch (width) {
            case 1:
                int v = instructions[pc + i / 2];
                return IntConstant.v((i % 2 == 0 ? v : v >> 8) & 0xff);
            case 2:
                v = instructions[pc + i];
                return IntConstant.v(v);
            case 4:
                v = instructions[pc + i * 2] + (instructions[pc + i * 2 + 1] << 16);
                return IntConstant.v(v);
            case 8:
                v = instructions[pc + i * 4] + (instructions[pc + i * 4 + 1] << 16);
                int w = instructions[pc + i * 4 + 2] + (instructions[pc + i * 4 + 3] << 16);
                return LongConstant.v((long) v + ((long) w << 32));
            default:
                throw new RuntimeException("Constant table illegal width " + width);
        }
    }

    private Stmt parseSparseStmt(JimpleLocal l, int base, int pc) {
        int size = c(pc);
        pc += 2;
        List<Value> keys = new ArrayList<Value>();
        for (int i = 0; i < size; i++)
            keys.add(IntConstant.v(integer(pc + i * 2)));
        pc += size * 2;
        List<Unit> table = new ArrayList<Unit>();
        for (int i = 0; i < size; i++)
            table.add(targetUnit(base + integer(pc + i * 2)));
        return new JLookupSwitchStmt(l, keys, table, targetUnit(base + 3));
    }

    private Stmt parsePackedStmt(Local l, int base, int pc) {
        int size = c(pc);
        int first_key = dI(pc);
        pc += 4;
        List<Unit> table = new ArrayList<Unit>();
        for (int i = 0; i < size; i++)
            table.add(targetUnit(base + integer(pc + i * 2)));
        return new JTableSwitchStmt(l, first_key, first_key + size - 1, table, targetUnit(base + 3));
    }

    private Unit targetUnit(int i) {
        TargetUnit u = new TargetUnit(i);
        targets.add(u);
        return u;
    }

    /**
     * Here we create the invoke statement. The invokeExpression must be
     * defined.
     * 
     * @param stmts
     * @param invokeExpr
     * @param pc
     * @return
     */
    private int emitInvoke(Stmt[] stmts, InvokeExpr invokeExpr, int pc) {
        pc += 3;
        switch (instructions[pc] & 0xff) {
            case 0x0a:
            case 0x0b:
            case 0x0c:
                stmts[pc] = new JNopStmt(); // Needed if target of a jump or
                                            // rather of a catch.
                stmts[pc - 3] = captureAssign(new JAssignStmt(locals[aa(pc)], invokeExpr), 0xa);
                pc++;
                break;
            default:
                stmts[pc - 3] = new JInvokeStmt(invokeExpr);
        }
        return pc;
    }

    /**
     * Degenerate case for the invoke statement, when we have no correct invoke
     * expression.
     * 
     * @param stmts
     * @param pc
     * @param typ : return type of invoke expr for correct assignment.
     * @return
     */
    private int emitNoInvoke(Stmt[] stmts, int pc, Type typ) {
        stmts[pc] = new JNopStmt();
        pc += 3;
        switch (instructions[pc] & 0xff) {
            case 0x0a:
            case 0x0b:
            case 0x0c:
                Constant dummy = (typ instanceof PrimType ? IntConstant.v(0) : NullConstant.v());
                stmts[pc] = captureAssign(new JAssignStmt(locals[aa(pc)], dummy), 0xa);
                pc++;
                break;
            default:
                D2JLog.warning("unexpected case begining at " + (pc - 3));
        }
        return pc;
    }

    /**
     * Compute the correct InvokeExpression from the method reference, parameter
     * lists and invokation mode. It may return null if the method reference
     * cannot be resolved.
     * 
     * @param sootMethodref
     * @param argList
     * @param selector
     * @return
     */
    private InvokeExpr buildInvokeExpr(SootMethodRef sootMethodref, List<Value> argList,
            int selector) {
        InvokeExpr ie;
        try {
            sootMethodref.resolve();
        } catch (Exception e) {
            df.unresolvedMethods.add(sootMethodref);
            return null;
        }
        switch (selector) {
            case 0:
                Value base = argList.get(0);
                argList.remove(0);

                if (sootMethodref.declaringClass().isInterface()) {
                    ie = new JInterfaceInvokeExpr(base, sootMethodref, pruneWideArgs(argList,
                            sootMethodref));
                } else {
                    ie = new JVirtualInvokeExpr(base, sootMethodref, pruneWideArgs(argList,
                            sootMethodref));
                }
                break;
            case 1:
                base = argList.get(0);

                argList.remove(0);
                ie = new JSpecialInvokeExpr((Local) base, sootMethodref, pruneWideArgs(argList,
                        sootMethodref));
                break;
            case 2:
                base = argList.get(0);
                argList.remove(0);
                ie = new JSpecialInvokeExpr((Local) base, sootMethodref, pruneWideArgs(argList,
                        sootMethodref));
                break;
            case 3:
                ie = new JStaticInvokeExpr(sootMethodref, pruneWideArgs(argList, sootMethodref));
                break;
            case 4:
                base = argList.get(0);
                argList.remove(0);
                List<Value> params = pruneWideArgs(argList, sootMethodref);
                try {
                    ie = new JInterfaceInvokeExpr(base, sootMethodref, params);
                } catch (RuntimeException e) {
                    D2JLog.warning(sootMethodref + "is not an interface (Called from " + method
                            + ")");
                    ie = new JVirtualInvokeExpr(base, sootMethodref, params);
                }
                break;
            default:
                throw new RuntimeException("Bad invoke expr");
        }
        if (ie instanceof InstanceInvokeExpr) {
            Type typ = sootMethodref.declaringClass().getType();
            typer.setType(((InstanceInvokeExpr) ie).getBaseBox(), typ);
        }
        int i = 0;
        for (Object typ : sootMethodref.parameterTypes()) {
            typer.setType(ie.getArgBox(i++), (Type) typ);
        }
        return ie;
    }

    @SuppressWarnings("unchecked")
    private List<Value> pruneWideArgs(List<Value> argList, SootMethodRef sootMethodref) {
        Iterator<Type> itTypes = sootMethodref.parameterTypes().iterator();
        Iterator<Value> itArgs = argList.iterator();
        while (itArgs.hasNext()) {
            itArgs.next();
            Type t = itTypes.next();
            if (t instanceof LongType || t instanceof DoubleType) {
                itArgs.next();
                itArgs.remove();
            }
        }
        if (itTypes.hasNext())
            throw new RuntimeException("Badly balanced args/params");
        return argList;
    }

    private int a(int pc) {
        return (instructions[pc] >> 8) & 0xf;
    }

    private int b(int pc) {
        return (instructions[pc] >> 12) & 0xf;
    }

    private int aa(int pc) {
        return (instructions[pc] >> 8) & 0xff;
    }

    private int c(int pc) {
        return instructions[pc + 1];
    }

    private int c1(int pc) {
        return instructions[pc + 1] & 0xff;
    }

    private int c2(int pc) {
        return (instructions[pc + 1] >> 8) & 0xff;
    }

    private int cI(int pc) {
        return instructions[pc + 1] + (instructions[pc + 2] << 16);
    }

    private long cL(int pc) {
        return (long) instructions[pc + 1] + ((long) instructions[pc + 2] << 16)
                + ((long) instructions[pc + 3] << 32) + ((long) instructions[pc + 4] << 48);
    }

    private int d(int pc) {
        return instructions[pc + 2];
    }

    private int dI(int pc) {
        return instructions[pc + 2] + (instructions[pc + 3] << 16);
    }

    private int d1(int pc) {
        return instructions[pc + 2] & 0xf;
    }

    private int d2(int pc) {
        return (instructions[pc + 2] >> 4) & 0xf;
    }

    private int d3(int pc) {
        return (instructions[pc + 2] >> 8) & 0xf;
    }

    private int d4(int pc) {
        return (instructions[pc + 2] >> 12) & 0xf;
    }

    private int integer(int pc) {
        return instructions[pc] + (instructions[pc + 1] << 16);
    }

    private void buildTraps(int start, int count, int offset) {
        int end = start + count;
        int saved = reader.getPos();
        reader.seek(handlers_offset + offset);
        int size = reader.sleb128();
        int asize = Math.abs(size);
        int type_idx, handler;
        JTrap trap;
        for (int i = 0; i < asize; i++) {
            type_idx = (int) reader.uleb128();
            Type raw_typ = findType(type_idx);
            if (raw_typ == null)
                continue;
            if (!(raw_typ instanceof RefType)) {
                D2JLog.warning("Trap type is not a ref type: " + raw_typ);
                continue;
            }
            RefType exc_typ = (RefType) raw_typ;
            SootClass exc_class = exc_typ.getSootClass();
            handler = (int) reader.uleb128();
            trap = new JTrap(exc_class, targetUnit(start), targetUnit(end), targetUnit(handler));
            if (D2JLog.instructions)
                D2JLog.info("Trap from " + start + " to " + end + " at " + handler);
            traps.add(trap);
        }
        if (size <= 0) {
            handler = (int) reader.uleb128();
            trap = new JTrap(throwableClass, targetUnit(start), targetUnit(end),
                    targetUnit(handler));
            if (D2JLog.instructions)
                D2JLog.info("Universal trap from " + start + " to " + end + " at " + handler);
            traps.add(trap);
        }
        reader.seek(saved);
    }

    private JimpleBody linkBody() {
        JimpleBody body = Jimple.v().newBody(method);
        PatchingChain<Unit> chain = body.getUnits();
        // Generate definition statements.
        int offArg = 0;
        if (!method.isStatic()) {
            RefType typ = method.getDeclaringClass().getType();
            JIdentityStmt idStmt = new JIdentityStmt(locals[registers_size - ins_size],
                    new ThisRef(typ));
            chain.addLast(idStmt);
            typer.setType(idStmt.leftBox, typ);
            offArg = 1;
        }

        int j = 0;
        for (int i = offArg; i < ins_size; i++) {
            Type typ = method.getParameterType(j);
            JIdentityStmt idStmt = new JIdentityStmt(locals[registers_size - ins_size + i],
                    new ParameterRef(typ, j++));
            if (typ instanceof LongType || typ instanceof DoubleType)
                i++;
            chain.addLast(idStmt);
            typer.setType(idStmt.leftBox, typ);
        }
        // Put the found statements in correct order and eventually expand them.
        for (int i = 0; i < stmts.length; i++) {
            Unit u = stmts[i];
            if (u != null) {
                if (u instanceof FillArrayStmt) {
                    stmts[i] = expandFillArrayStmt((FillArrayStmt) u, chain, i);
                } else {
                    u.addTag(new BytecodeOffsetTag(i));
                    chain.addLast(u);
                }
            }
        }
        // Add the exceptions
        body.getTraps().addAll(traps);

        // Here we put the right destination.
        for (TargetUnit tgtDescr : targets) {
            int pos = tgtDescr.getPosition();

            if (pos == stmts.length)
                pos = pos - 1;
            // This is a trap hack. We have to check.
            while (stmts[pos] == null) {
                pos = pos - 1;
            }
            Unit tgt = stmts[pos];
            if (tgt == null) {
                for (int i = 0; i < stmts.length; i++) {
                    System.err.println(i + ":  " + stmts[i]);
                }

                throw new RuntimeException("No instruction at " + pos + " in method " + method);
            }
            tgtDescr.redirectJumpsToThisTo(tgt);
        }

        // Type the exceptions
        for (JTrap trap : traps) {
            Unit u = trap.getHandlerUnit();
            if (u instanceof JIdentityStmt) {
                ValueBox box = ((JIdentityStmt) u).getLeftOpBox();
                typer.setType(box, trap.getException().getType());
            }
        }
        Chain<Local> body_locals = body.getLocals();
        for (Local loc : locals)
            body_locals.addLast(loc);
        for (Local loc : auxLocals)
            body_locals.addLast(loc);
        method.setActiveBody(body);
        return body;
    }

    /**
     * Transform the Dalvik special fill-array or fill-new-array statement in a
     * list of regular jimple statements.
     * 
     * @param fas the statement to expand.
     * @param chain the chain to add the result of the expansion to
     * @param bcOffset
     * @return the first statement of the generated list
     */
    private Stmt expandFillArrayStmt(FillArrayStmt fas, PatchingChain<Unit> chain, int bcOffset) {
        Stmt result = null;
        Local lhs = fas.getLhs();
        ArrayType typ = fas.getArrayType();
        List<Value> contents = fas.getContents();
        if (contents.contains(lhs)) {
            // aux declared as local
            Local aux = new JimpleLocal("aux" + auxLocals.size(), UnknownType.v());
            auxLocals.add(aux);
            // substitution of lhs into aux in contents.
            List<Value> newContents = new ArrayList<Value>();
            for (Value v : contents)
                newContents.add(v.equals(lhs) ? aux : v);
            contents = newContents;
            // Preserve initial value of lhs in aux.
            Stmt stmt = captureAssign(new JAssignStmt(aux, lhs), 0x1);
            stmt.addTag(new BytecodeOffsetTag(bcOffset));
            chain.addLast(stmt);
            result = stmt;
        }
        if (typ != null) {
            int length = contents.size();
            Stmt stmt = captureAssign(new JAssignStmt(lhs, new JNewArrayExpr(typ.getElementType(),
                    IntConstant.v(length))), 0x24);
            stmt.addTag(new BytecodeOffsetTag(bcOffset));
            chain.addLast(stmt);
            if (result == null)
                result = stmt;
        }
        int j = 0;
        for (Value c : contents) {
            Stmt stmt = new JAssignStmt(new JArrayRef(lhs, IntConstant.v(j)), c);
            stmt.addTag(new BytecodeOffsetTag(bcOffset));
            j++;
            if (result == null)
                result = stmt;
            chain.addLast(stmt);
        }
        return result;
    }
}
