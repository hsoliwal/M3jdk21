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
package com.sun.tools.javac.m3;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Opt-in compiler-owned publication of the exact parser input and its existing result.
 * This target-owned seam has no Synexia, Maven, Gradle, JNI or other module dependency.
 * It does no work beyond the context lookup when no observer has been installed.
 */
public final class M3ASTPC {
    private static final Context.Key<State> KEY = new Context.Key<>();
    private M3ASTPC() { }

    private static final class State {
        final List<BiConsumer<CompilationUnitTree, CharSequence>> observers = new ArrayList<>();
    }

    private static final class Subscription implements AutoCloseable {
        private State owner;
        private BiConsumer<CompilationUnitTree, CharSequence> observer;
        Subscription(State owner, BiConsumer<CompilationUnitTree, CharSequence> observer) {
            this.owner = owner;
            this.observer = observer;
        }
        @Override public synchronized void close() {
            if (owner == null) return;
            synchronized (owner) {
                for (int i = 0; i < owner.observers.size(); i++) {
                    if (owner.observers.get(i) == observer) { owner.observers.remove(i); break; }
                }
            }
            owner = null;
            observer = null;
        }
    }

    /** Register before parsing; the returned handle releases the observer without global state. */
    public static AutoCloseable observe(JavacTask task,
            BiConsumer<CompilationUnitTree, CharSequence> observer) {
        Objects.requireNonNull(observer, "observer");
        if (!(task instanceof BasicJavacTask javac)) {
            throw new IllegalArgumentException("M3ASTPC requires the owning javac task");
        }
        Context context = javac.getContext();
        State state = context.get(KEY);
        if (state == null) { state = new State(); context.put(KEY, state); }
        State owner = state;
        synchronized (owner) { owner.observers.add(observer); }
        return new Subscription(owner, observer);
    }

    /** Actual language level after javac has applied --release/-source options. */
    public static int sourceRelease(JavacTask task) {
        if (!(task instanceof BasicJavacTask javac)) throw new IllegalArgumentException("foreign compiler task");
        String name = Source.instance(javac.getContext()).name;
        return Integer.parseInt(name.startsWith("1.") ? name.substring(2) : name);
    }

    /** Actual diagnostics authority for the owning task; an unknown count is not success. */
    public static int errorCount(JavacTask task) {
        if (!(task instanceof BasicJavacTask javac)) throw new IllegalArgumentException("foreign compiler task");
        return Log.instance(javac.getContext()).nerrors;
    }

    /** Compiler hook immediately before finished(PARSE); retains no tree or source afterwards. */
    public static void parsed(Context context, CompilationUnitTree unit, CharSequence source) {
        State state = context.get(KEY);
        if (state == null || source == null) return;
        List<BiConsumer<CompilationUnitTree, CharSequence>> observers;
        synchronized (state) {
            if (state.observers.isEmpty()) return;
            observers = List.copyOf(state.observers);
        }
        String stable = source.toString();
        for (BiConsumer<CompilationUnitTree, CharSequence> observer : observers) {
            observer.accept(unit, stable);
        }
    }
}
