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
package com.sun.source.util;

/*
 * Modified 2026-10-21 by Synexia (Hitesh Soliwal and contributors): added
 * immutable, explicitly partial build facts from the original M3AST event.
 * Modified 2026-10-22: added detached, typed method facts and source-site relations.
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.m3.M3ASTPC;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import javax.lang.model.element.TypeElement;

/**
 * An opt-in, compilation-owned view of javac's existing frontend work.
 *
 * <p>This compiler API does not parse, attribute, or persist a second tree.
 * Consumers run serially at the end of each parse or type-analysis event. A
 * {@link Unit} is valid only on the callback thread and only until those
 * callbacks return. Retain an immutable projection, not a Unit, Tree, Trees,
 * Element, TypeMirror, or compiler task, for subsequent or out-of-process use.
 *
 * <p>A projection key has identity semantics and is memoized only within one
 * event. Different types, phases, compilation tasks, and source generations
 * never share projected values automatically. A source digest is not a key
 * for reusing attributed facts across classpaths, toolchains, or processors.
 * Existing M3 file/AST cache providers own any bounded persistent snapshots.
 * An enabled handle seals the exact character input once per parse, retaining
 * only URI/digest metadata after the parse callbacks; later source requests
 * reject edits made since parsing.
 *
 * <p>Register all consumers before starting the task. Closing this handle
 * removes its listener and releases compiler references. Task.call() closes
 * it at the COMPILATION event; callers using parse(), analyze(), and generate()
 * separately must close it themselves. Such closure reports completed=false
 * because those methods do not emit COMPILATION events.
 */
public interface M3AST extends AutoCloseable {
    /** Obtain the single M3 handle owned by this javac task. */
    static M3AST open(JavacTask task) {
        return M3ASTPC.open(task, false);
    }

    /**
     * Obtain the handle, optionally retaining comments/end positions and
     * disabling parser string folding before parsing starts. Lossless tree
     * conversion additionally requires a compatible converter and type context.
     */
    static M3AST open(JavacTask task, boolean retainSyntax) {
        return M3ASTPC.open(task, retainSyntax);
    }

    /**
     * Whether this task's already-parsed options select the builtin M3AST plugin.
     * This query creates no M3 handle, listener, compiler or source work. It
     * reports selection, not successful plugin initialization or compilation.
     * Call on the task's owning thread while its compiler context is available.
     *
     * @param task the original javac task whose options have been processed
     * @return whether the existing plugin options select the exact name M3AST
     * @throws NullPointerException if task is null
     * @throws IllegalArgumentException if task is not a javac implementation
     * @throws IllegalStateException if the task context has been released or
     *         its parsed arguments are unavailable
     */
    static boolean isPluginSelected(JavacTask task) {
        return M3ASTPC.isPluginSelected(task);
    }

    /**
     * Project partial build facts from this original ANALYZED event. Consumers
     * share one result by using this entry point; no parsing, attribution or
     * source reread is performed. The result never certifies a complete build
     * dependency graph or authorizes skipping a compiler or plugin execution.
     *
     * @throws IllegalStateException if the unit is not a live ANALYZED event
     * @throws IllegalArgumentException if the unit is not owned by this compiler
     * @throws Exception if the event projection cannot be captured
     */
    static BuildFacts buildFacts(Unit unit) throws Exception {
        return M3ASTPC.buildFacts(Objects.requireNonNull(unit, "unit"));
    }

    /**
     * Project typed method facts from the original live ANALYZED event. The
     * existing event cache shares one detached result among consumers; no
     * parsing, attribution or source reread is performed. The projection is
     * bounded to 65,536 method rows, 1,048,576 relation sites and 131,072 unique
     * identities with at most 8,388,608 UTF-16 identity characters per event.
     * Exceeding a budget refuses the whole projection, without truncation.
     *
     * @param unit the original live compiler event
     * @return detached method facts for its analyzed top-level declaration
     * @throws NullPointerException if unit is null
     * @throws IllegalStateException if the event is not live and ANALYZED,
     *         or the declared projection budget is exceeded
     * @throws IllegalArgumentException if the unit belongs to another compiler
     * @throws Exception if the projection cannot be captured
     */
    static MethodFacts methodFacts(Unit unit) throws Exception {
        return M3ASTPC.methodFacts(Objects.requireNonNull(unit, "unit"));
    }

    /** Register a consumer once, by object identity, before the first event. */
    void addConsumer(Consumer consumer);

    /** Obtain counters; these are event/work counts, not performance claims. */
    Summary summary();

    /**
     * Idempotently release this session on the task thread. If called inside
     * a callback, invalidates its view and stops dispatch to later consumers.
     */
    @Override void close();

    /** The currently valid compiler facts. */
    enum Phase { PARSED, ANALYZED }

    /** A projection of this exact live event. */
    @FunctionalInterface
    interface Factory<T> {
        T create(Unit unit) throws Exception;
    }

    /**
     * A caller-owned reusable operation identity. Reuse the same instance to
     * share an operation among consumers. Equal names alone do not share work.
     */
    final class Projection<T> {
        private final String name;
        private final Factory<T> factory;

        public Projection(String name, Factory<T> factory) {
            this.name = Objects.requireNonNull(name, "name");
            this.factory = Objects.requireNonNull(factory, "factory");
        }

        public String name() { return name; }

        /** Called by the compilation owner after memoization admission. */
        public T create(Unit unit) throws Exception { return factory.create(unit); }
    }

    /**
     * Retained character source. The hash covers exact UTF-16 code units in
     * big-endian order, including unpaired surrogates; it is not a byte-file hash.
     */
    record Source(URI uri, String text, String utf16Sha256) {
        public Source {
            Objects.requireNonNull(uri, "uri");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(utf16Sha256, "utf16Sha256");
        }
    }

    /**
     * Detached facts for one analyzed top-level type and its visited nested,
     * local and anonymous declarations. Names use JVM binary-name spelling.
     * Resolved member and constant uses include the declaring type even when
     * javac can inline the value into bytecode. Same-event declared names are
     * omitted from referenced names. Both name lanes are sorted and unique.
     *
     * <p>The source seal is the original parser's exact UTF-16 generation, not
     * a filesystem byte hash or a portable semantic-context key. These facts
     * omit negative/wildcard lookup dependencies, processor inputs, output
     * ownership and plugin effects. They describe an analysis event, including
     * a possible failed compilation, rather than its successful publication.
     * Other top-level types in the source have separate analysis events.
     */
    record BuildFacts(URI sourceUri, int sourceLength, String sourceUtf16Sha256,
                      String analyzedBinaryName, String[] declaredBinaryNames,
                      String[] referencedBinaryNames, boolean hasUnresolvedSymbols) {
        public static final String SCHEMA = "M3AST_BUILD_FACTS_V1";

        public BuildFacts {
            Objects.requireNonNull(sourceUri, "sourceUri");
            Objects.requireNonNull(sourceUtf16Sha256, "sourceUtf16Sha256");
            Objects.requireNonNull(analyzedBinaryName, "analyzedBinaryName");
            if (sourceLength < 0 || sourceUtf16Sha256.length() != 64
                    || analyzedBinaryName.isEmpty())
                throw new IllegalArgumentException("invalid build fact source identity");
            for (int i = 0; i < sourceUtf16Sha256.length(); i++) {
                char c = sourceUtf16Sha256.charAt(i);
                if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f'))
                    throw new IllegalArgumentException("invalid build fact source digest");
            }
            declaredBinaryNames = names(declaredBinaryNames);
            referencedBinaryNames = names(referencedBinaryNames);
        }

        @Override public String[] declaredBinaryNames() { return declaredBinaryNames.clone(); }
        @Override public String[] referencedBinaryNames() { return referencedBinaryNames.clone(); }

        /** This event projection deliberately cannot certify complete dependency coverage. */
        public boolean completeDependencyGraph() { return false; }

        private static String[] names(String[] input) {
            String[] values = Objects.requireNonNull(input, "binary names").clone();
            String previous = null;
            for (String value : values) {
                if (value == null || value.isEmpty()
                        || (previous != null && previous.compareTo(value) >= 0))
                    throw new IllegalArgumentException("noncanonical build fact names");
                previous = value;
            }
            return values;
        }
    }

    /**
     * Detached, columnar observations of explicit source methods and
     * constructors in one analyzed top-level type, including member, local
     * and anonymous classes. Rows follow compiler tree traversal order, not
     * a domain ranking. Generated constructors, bridges and record members
     * are excluded. Lambda bodies are not method rows; explicit methods in
     * classes declared inside a lambda remain separate rows.
     *
     * <p>Body counters describe source-positioned sites in the attributed
     * tree. They exclude nested class and lambda bodies, implicit constructor
     * invocations, class/instance initializers, and annotation/default-value
     * expressions. Block depth counts the method body as one. Branch counts
     * are separate syntactic constructs, not cyclomatic complexity or a CFG.
     * Method flags and formal parameter counts are attributed declarations;
     * a compact record constructor therefore has its implicit formal parameters.
     *
     * <p>Presence masks distinguish missing observations from zero. Missing
     * parser end positions leave source ranges or all body observations
     * absent. Abstract/native declarations have no body observations. Field
     * counts and relations include only resolved field sites: a compound
     * assignment or increment is both a read and write; an array-element
     * write reads the field holding that array but does not write the field.
     * No statement here implies purity, escape behavior, allocation at run
     * time, executed effects, or a complete dependency/call graph.
     *
     * <p>Identity dictionary strings are sorted and unique. Class identities
     * use binary names. A method identity is {@code owner#name(descriptor)return}
     * and a field identity is {@code owner#name:descriptor}, with JVM type
     * descriptors from the erasure of the already-resolved symbol's type.
     * These identify the observed symbol, not a closed-world dispatch target;
     * a signature-polymorphic symbol can describe a call-site signature.
     * Missing or unrepresentable identities set {@link #UNRESOLVED} and do
     * not invent an edge. Resolved array pseudo-members have no class identity.
     *
     * <p>Each CSR relation has rowCount+1 offsets, starts at zero and ends
     * at its edge-lane length. Duplicate sites are retained in compiler
     * traversal order, not execution order. CALL includes method invocations
     * and new-class constructor sites; MEMBER_REFERENCE is separate because
     * creating a method reference does not invoke its target. Every edge has
     * a member identity ID and a declaring-class identity ID.
     *
     * <p>The source seal binds exact original UTF-16, not a filesystem byte
     * digest. The compilerContextId is an opaque per-task UUID, shared across
     * that task's events and never reused automatically across tasks. It is
     * not a reproducible digest of classpath, processors or toolchain, nor a
     * certificate of producer authenticity. Consumers need their own complete
     * semantic-context policy for persistent reuse. This result retains no
     * compiler object, source text, per-row wrapper, parser or runtime service.
     */
    final class MethodFacts {
        /** Versioned schema for these explicit column and relation IDs. */
        public static final String SCHEMA = "M3AST_METHOD_FACTS_V1";
        /** Long column of attributed declaration flags. */
        public static final int DECLARATION_FLAGS = 0;
        /** Attributed formal parameter count, including compact-record formals. */
        public static final int PARAMETER_COUNT = 1;
        /** UTF-16 declaration start, inclusive. */
        public static final int SOURCE_START = 2;
        /** UTF-16 declaration end, exclusive. */
        public static final int SOURCE_END = 3;
        /** UTF-16 body start, inclusive. */
        public static final int BODY_START = 4;
        /** UTF-16 body end, exclusive. */
        public static final int BODY_END = 5;
        /** Number of source if statements in this body. */
        public static final int IF_COUNT = 6;
        /** Number of source conditional expressions in this body. */
        public static final int CONDITIONAL_COUNT = 7;
        /** Number of switch statements and switch expressions. */
        public static final int SWITCH_COUNT = 8;
        /** Number of for, enhanced-for, while and do loops. */
        public static final int LOOP_COUNT = 9;
        /** Maximum nested source block depth, including the body as one. */
        public static final int MAX_BLOCK_DEPTH = 10;
        /** Source method invocations, including explicit this/super calls. */
        public static final int INVOCATION_COUNT = 11;
        /** Source new-class expressions; not a runtime allocation count. */
        public static final int NEW_CLASS_COUNT = 12;
        /** Source new-array expressions, including array initializers. */
        public static final int NEW_ARRAY_COUNT = 13;
        /** Source throw statements. */
        public static final int THROW_COUNT = 14;
        /** Source synchronized statements; the method modifier is separate. */
        public static final int SYNCHRONIZED_COUNT = 15;
        /** Resolved source field-read sites. */
        public static final int FIELD_READ_COUNT = 16;
        /** Resolved source field-write sites. */
        public static final int FIELD_WRITE_COUNT = 17;
        /** Lambda expression sites, without visiting their bodies. */
        public static final int LAMBDA_COUNT = 18;
        /** Method-reference sites, without invoking the referenced method. */
        public static final int MEMBER_REFERENCE_COUNT = 19;
        /** Number of source declared throws entries. */
        public static final int DECLARED_THROWS_COUNT = 20;
        /** One if an observed symbol/type cannot be represented, otherwise zero. */
        public static final int UNRESOLVED = 21;
        /** Dictionary ID of this row's resolved method identity. */
        public static final int METHOD_ID = 22;
        /** Dictionary ID of this row's declaring binary class name. */
        public static final int OWNER_ID = 23;
        /** Number of stable column IDs in this schema. */
        public static final int COLUMN_COUNT = 24;

        /** Public method. */
        public static final long FLAG_PUBLIC = 0x0001L;
        /** Private method. */
        public static final long FLAG_PRIVATE = 0x0002L;
        /** Protected method. */
        public static final long FLAG_PROTECTED = 0x0004L;
        /** Static method. */
        public static final long FLAG_STATIC = 0x0008L;
        /** Final method. */
        public static final long FLAG_FINAL = 0x0010L;
        /** Synchronized method. */
        public static final long FLAG_SYNCHRONIZED = 0x0020L;
        /** Variable-arity method. */
        public static final long FLAG_VARARGS = 0x0080L;
        /** Native method. */
        public static final long FLAG_NATIVE = 0x0100L;
        /** Abstract method. */
        public static final long FLAG_ABSTRACT = 0x0400L;
        /** Strict floating-point modifier retained by the compiler. */
        public static final long FLAG_STRICTFP = 0x0800L;
        /** Default interface method; this is a schema bit, not a classfile flag. */
        public static final long FLAG_DEFAULT = 0x10000L;

        /** Resolved invocation and new-class constructor sites. */
        public static final int CALL = 0;
        /** Resolved field-read sites. */
        public static final int FIELD_READ = 1;
        /** Resolved field-write sites. */
        public static final int FIELD_WRITE = 2;
        /** Resolved method-reference sites, distinct from CALL. */
        public static final int MEMBER_REFERENCE = 3;
        /** Number of stable CSR relation IDs. */
        public static final int RELATION_COUNT = 4;

        private static final String[] COLUMN_NAMES = {
            "declaration_flags", "parameter_count", "source_start", "source_end",
            "body_start", "body_end", "if_count", "conditional_count", "switch_count",
            "loop_count", "max_block_depth", "invocation_count", "new_class_count",
            "new_array_count", "throw_count", "synchronized_count", "field_read_count",
            "field_write_count", "lambda_count", "member_reference_count",
            "declared_throws_count", "unresolved", "method_id", "owner_id"
        };
        private static final long FLAGS_MASK = FLAG_PUBLIC | FLAG_PRIVATE | FLAG_PROTECTED
                | FLAG_STATIC | FLAG_FINAL | FLAG_SYNCHRONIZED | FLAG_VARARGS | FLAG_NATIVE
                | FLAG_ABSTRACT | FLAG_STRICTFP | FLAG_DEFAULT;
        private final URI sourceUri;
        private final int sourceLength;
        private final String sourceUtf16Sha256;
        private final String compilerContextId;
        private final String analyzedBinaryName;
        private final String[] identities;
        private final long[] flags;
        private final int[][] intColumns;
        private final long[] presenceMasks;
        private final int[][] relationOffsets;
        private final int[][] relationIds;
        private final int[][] relationOwnerIds;

        /**
         * Construct a detached projection, defensively copying all array lanes.
         * Integer lane index is columnId-1; flags are the sole long column.
         * Absent integer values are zero-filled and must be read with their
         * presence mask. Geometry validation does not certify a caller's facts.
         *
         * @param sourceUri original source URI
         * @param sourceLength original UTF-16 length
         * @param sourceUtf16Sha256 original lowercase SHA-256 seal
         * @param compilerContextId canonical per-task UUID string
         * @param analyzedBinaryName analyzed top-level binary name
         * @param identities sorted unique identity dictionary
         * @param flags long declaration-flags lane
         * @param intColumns integer lanes indexed by columnId-1
         * @param presenceMasks one presence-bit mask per method row
         * @param relationOffsets four CSR offset lanes
         * @param relationIds four CSR member-identity lanes
         * @param relationOwnerIds four CSR declaring-owner lanes
         * @throws NullPointerException if any required input is null
         * @throws IllegalArgumentException if identity, geometry or values are invalid
         */
        public MethodFacts(URI sourceUri, int sourceLength, String sourceUtf16Sha256,
                           String compilerContextId, String analyzedBinaryName,
                           String[] identities, long[] flags, int[][] intColumns,
                           long[] presenceMasks, int[][] relationOffsets,
                           int[][] relationIds, int[][] relationOwnerIds) {
            this.sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");
            this.sourceLength = sourceLength;
            this.sourceUtf16Sha256 = Objects.requireNonNull(sourceUtf16Sha256, "sourceUtf16Sha256");
            this.compilerContextId = Objects.requireNonNull(compilerContextId, "compilerContextId");
            this.analyzedBinaryName = Objects.requireNonNull(analyzedBinaryName, "analyzedBinaryName");
            if (sourceLength < 0 || sourceUtf16Sha256.length() != 64 || analyzedBinaryName.isEmpty()
                    || !UUID.fromString(compilerContextId).toString().equals(compilerContextId))
                throw new IllegalArgumentException("invalid method fact source/context identity");
            for (int i = 0; i < sourceUtf16Sha256.length(); i++) {
                char c = sourceUtf16Sha256.charAt(i);
                if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'f'))
                    throw new IllegalArgumentException("invalid method fact source digest");
            }
            this.identities = BuildFacts.names(identities);
            this.flags = Objects.requireNonNull(flags, "flags").clone();
            this.presenceMasks = Objects.requireNonNull(presenceMasks, "presenceMasks").clone();
            this.intColumns = copy(intColumns, COLUMN_COUNT - 1, "intColumns");
            this.relationOffsets = copy(relationOffsets, RELATION_COUNT, "relationOffsets");
            this.relationIds = copy(relationIds, RELATION_COUNT, "relationIds");
            this.relationOwnerIds = copy(relationOwnerIds, RELATION_COUNT, "relationOwnerIds");
            int rows = this.flags.length;
            if (this.presenceMasks.length != rows) throw new IllegalArgumentException("method mask geometry");
            for (int[] column : this.intColumns)
                if (column.length != rows) throw new IllegalArgumentException("method column geometry");
            long required = (1L << DECLARATION_FLAGS) | (1L << PARAMETER_COUNT) | (1L << UNRESOLVED);
            for (int row = 0; row < rows; row++) {
                long mask = this.presenceMasks[row];
                if ((mask >>> COLUMN_COUNT) != 0 || (mask & required) != required
                        || (this.flags[row] & ~FLAGS_MASK) != 0)
                    throw new IllegalArgumentException("method flag/presence mask");
                for (int column = 1; column < COLUMN_COUNT; column++) {
                    int value = this.intColumns[column - 1][row];
                    if (value < 0 || (!hasValue(row, column) && value != 0)
                            || (column == UNRESOLVED && value > 1)
                            || (hasValue(row, column) && column >= METHOD_ID && value >= this.identities.length)
                            || (hasValue(row, column) && column >= SOURCE_START && column <= BODY_END
                                && value > sourceLength))
                        throw new IllegalArgumentException("invalid method column value");
                }
                range(row, SOURCE_START, SOURCE_END);
                range(row, BODY_START, BODY_END);
            }
            for (int relation = 0; relation < RELATION_COUNT; relation++) {
                int[] offsets = this.relationOffsets[relation], ids = this.relationIds[relation];
                int[] owners = this.relationOwnerIds[relation];
                if (offsets.length != (long) rows + 1 || offsets[0] != 0
                        || offsets[rows] != ids.length || ids.length != owners.length)
                    throw new IllegalArgumentException("method relation geometry");
                for (int row = 0; row < rows; row++)
                    if (offsets[row] > offsets[row + 1] || offsets[row] < 0)
                        throw new IllegalArgumentException("nonmonotone method relation offsets");
                for (int edge = 0; edge < ids.length; edge++)
                    if (ids[edge] < 0 || ids[edge] >= this.identities.length
                            || owners[edge] < 0 || owners[edge] >= this.identities.length)
                        throw new IllegalArgumentException("invalid method relation identity");
            }
        }

        private static int[][] copy(int[][] input, int count, String label) {
            Objects.requireNonNull(input, label);
            if (input.length != count) throw new IllegalArgumentException(label + " lane count");
            int[][] result = new int[count][];
            for (int i = 0; i < count; i++) result[i] = Objects.requireNonNull(input[i], label).clone();
            return result;
        }

        private void range(int row, int start, int end) {
            if (hasValue(row, start) && hasValue(row, end)
                    && intColumns[start - 1][row] > intColumns[end - 1][row])
                throw new IllegalArgumentException("inverted method source range");
        }

        /** @return the original source URI */
        public URI sourceUri() { return sourceUri; }
        /** @return the original source's UTF-16 length */
        public int sourceLength() { return sourceLength; }
        /** @return the original source's exact UTF-16 SHA-256 seal */
        public String sourceUtf16Sha256() { return sourceUtf16Sha256; }
        /** @return the opaque per-task discriminator, not a portable semantic key */
        public String compilerContextId() { return compilerContextId; }
        /** @return the analyzed top-level binary name */
        public String analyzedBinaryName() { return analyzedBinaryName; }
        /** @return the number of explicit source method rows */
        public int rowCount() { return flags.length; }
        /** @return a defensive copy of the sorted identity dictionary */
        public String[] identities() { return identities.clone(); }
        /**
         * @param id dictionary ID
         * @return its immutable identity string
         */
        public String identity(int id) { return identities[Objects.checkIndex(id, identities.length)]; }
        /** @return a defensive copy of the per-row presence masks */
        public long[] presenceMasks() { return presenceMasks.clone(); }
        /**
         * @param column stable column ID
         * @return its schema name
         */
        public static String columnName(int column) { column(column); return COLUMN_NAMES[column]; }
        /**
         * @param row method row
         * @param column stable column ID
         * @return whether the observation is present
         */
        public boolean hasValue(int row, int column) {
            column(column);
            return (presenceMasks[Objects.checkIndex(row, rowCount())] & (1L << column)) != 0;
        }
        /**
         * @param row method row
         * @param column integer column ID
         * @return the known value
         */
        public int intValue(int row, int column) {
            intColumnId(column);
            if (!hasValue(row, column)) throw new IllegalStateException("method fact value is absent");
            return intColumns[column - 1][row];
        }
        /**
         * @param row method row
         * @param column long column ID
         * @return the known value
         */
        public long longValue(int row, int column) {
            longColumnId(column);
            if (!hasValue(row, column)) throw new IllegalStateException("method fact value is absent");
            return flags[row];
        }
        /**
         * @param column integer column ID
         * @return a defensive raw lane copy; consult presence for zero-filled missing values
         */
        public int[] intColumn(int column) { intColumnId(column); return intColumns[column - 1].clone(); }
        /**
         * @param column long column ID
         * @return a defensive raw lane copy
         */
        public long[] longColumn(int column) { longColumnId(column); return flags.clone(); }
        /**
         * @param relation relation ID
         * @return a defensive CSR offset copy
         */
        public int[] relationOffsets(int relation) { relation(relation); return relationOffsets[relation].clone(); }
        /**
         * @param relation relation ID
         * @return a defensive CSR member-identity copy
         */
        public int[] relationIds(int relation) { relation(relation); return relationIds[relation].clone(); }
        /**
         * @param relation relation ID
         * @return a defensive CSR declaring-owner copy
         */
        public int[] relationOwnerIds(int relation) { relation(relation); return relationOwnerIds[relation].clone(); }
        /** @return whether any row contains an unresolved or unrepresentable observed symbol */
        public boolean hasUnresolvedSymbols() {
            for (int value : intColumns[UNRESOLVED - 1]) if (value != 0) return true;
            return false;
        }
        /** @return false; source-site observations cannot certify a complete call graph */
        public boolean completeCallGraph() { return false; }

        private static void column(int column) {
            if (column < 0 || column >= COLUMN_COUNT) throw new IllegalArgumentException("method column ID");
        }
        private static void intColumnId(int column) {
            column(column);
            if (column == DECLARATION_FLAGS) throw new IllegalArgumentException("method column is long");
        }
        private static void longColumnId(int column) {
            if (column != DECLARATION_FLAGS) throw new IllegalArgumentException("method column is not long");
        }
        private static void relation(int relation) {
            if (relation < 0 || relation >= RELATION_COUNT) throw new IllegalArgumentException("method relation ID");
        }
    }

    /** A scoped reference to the actual compiler tree, with lazy projections. */
    interface Unit {
        Phase phase();
        CompilationUnitTree tree();
        Trees trees();
        /** The analyzed top-level type, or null for a PARSED event. */
        TypeElement analyzedType();
        /** Unique in-memory compilation identity; not a portable semantic key. */
        Object contextIdentity();
        /** Uses the exact parsed source at PARSE; later reads are checked against its source seal. */
        Source source() throws IOException;
        /**
         * Compute once per identity and event. Null and thrown exceptions are
         * memoized too. Recursive requests for the same key are refused.
         */
        <T> T project(Projection<T> projection) throws Exception;
    }

    /** Service-provider or explicitly registered frontend consumer. */
    interface Consumer {
        default void parsed(Unit unit) throws Exception { }
        default void analyzed(Unit unit) throws Exception { }
        default void finished(Summary summary) throws Exception { }
    }

    /** Immutable, per-compilation operation counts and completion disposition. */
    record Summary(long parsedUnits, long analyzedTypes, long capturedSources, long sourceReads,
                   long projectionComputations, long projectionHits,
                   int compilerErrors, boolean completed, boolean closed,
                   boolean consumerFailed) { }
}
