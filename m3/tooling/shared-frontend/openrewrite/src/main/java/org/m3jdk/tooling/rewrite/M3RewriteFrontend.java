/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package org.m3jdk.tooling.rewrite;

import com.sun.source.util.M3AST;
import com.sun.tools.javac.comp.CompileStates;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.m3.M3ASTPC;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.isolated.ReloadableJava21ParserVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

/**
 * Optional OpenRewrite 8.17.1 / javac 21 conversion of an existing compiler unit.
 * This adapter uses the upstream visitor directly in the active compiler's
 * loader. It does not create a parser, compiler task, or attribution pass.
 */
public final class M3RewriteFrontend {
    private M3RewriteFrontend() { }

    /**
     * Create a caller-owned projection identity with a compilation-local type
     * cache. Share this exact key among consumers; discard it after the task.
     */
    public static M3AST.Projection<J.CompilationUnit> projection(ExecutionContext execution) {
        Objects.requireNonNull(execution, "execution");
        JavaTypeCache types = new JavaTypeCache();
        Object[] owner = new Object[1];
        return new M3AST.Projection<>("OpenRewrite-8.17.1-Java21", unit -> {
            Object identity = unit.contextIdentity();
            if (owner[0] == null) owner[0] = identity;
            else if (owner[0] != identity)
                throw new IllegalStateException("Rewrite projection belongs to a different compilation");
            return convert(unit, types, execution);
        });
    }

    private static J.CompilationUnit convert(M3AST.Unit unit, JavaTypeCache types,
                                            ExecutionContext execution) throws IOException {
        if (unit.phase() != M3AST.Phase.ANALYZED)
            throw new IllegalStateException("Rewrite conversion requires attributed compiler input");
        if (!M3ASTPC.retainsSyntax(unit))
            throw new IllegalStateException("Enable M3AST lossless before compilation");
        if (!(unit.tree() instanceof JCTree.JCCompilationUnit original))
            throw new IllegalStateException("Rewrite converter and javac tree use different compiler loaders");
        var context = M3ASTPC.contextOf(unit);
        if (Log.instance(context).nerrors != 0)
            throw new IllegalStateException("Rewrite conversion refuses compiler errors");
        if (original.endPositions == null)
            throw new IllegalStateException("Rewrite conversion requires retained end positions");
        // Ordinary javac can lower one top-level class before attributing the
        // next. Never claim a partially attributed or lowered unit is an LST.
        // -XDcompilePolicy=byfile, or explicit parse/analyze before generate,
        // makes complete units available without creating a second frontend.
        var states = CompileStates.instance(context);
        var enter = Enter.instance(context);
        int declarations = 0;
        for (JCTree definition : original.defs) {
            if (definition instanceof JCTree.JCClassDecl declaration) {
                declarations++;
                var environment = declaration.sym == null ? null : enter.getEnv(declaration.sym);
                if (environment == null || !states.isDone(environment, CompileStates.CompileState.ATTR))
                    throw new IllegalStateException("Rewrite unit is not fully attributed; use compilePolicy=byfile");
                if (states.isDone(environment, CompileStates.CompileState.TRANSTYPES))
                    throw new IllegalStateException("Rewrite conversion refuses an already lowered unit");
            }
        }
        if (declarations == 0)
            throw new IllegalStateException("This adapter requires an analyzed top-level type");
        M3AST.Source source = unit.source();
        Path path = Path.of(source.uri().getPath());
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(source.text()));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        J.CompilationUnit result;
        try (var input = new EncodingDetectingInputStream(
                new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            var visitor = new ReloadableJava21ParserVisitor(path, null, input,
                    List.of(), types, execution, context);
            result = (J.CompilationUnit) visitor.scan(original, Space.EMPTY);
        }
        if (result == null || !result.printAll().equals(source.text()))
            throw new IOException("Rewrite conversion changed retained source: " + source.uri());
        return result;
    }
}
