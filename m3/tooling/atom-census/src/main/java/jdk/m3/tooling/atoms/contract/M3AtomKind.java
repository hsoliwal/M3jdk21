/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

/**
 * Closed atom-kind vocabulary used by the M3 repository census and coverage ledger.
 *
 * <p>The vocabulary is deliberately language-aware. A method, JNI export, HotSpot
 * architecture rule and build rule do not require identical proof, but each still
 * receives one stable atom identity and an explicit evidence state.</p>
 */
public enum M3AtomKind {
    /** One tracked repository file, including files that contain no smaller executable atom. */
    FILE_CONTAINER(false, false, false),
    /** Java method with a body. */
    JAVA_METHOD(true, true, false),
    /** Java constructor with a body. */
    JAVA_CONSTRUCTOR(true, true, false),
    /** Java static initializer block. */
    JAVA_STATIC_INITIALIZER(true, true, false),
    /** Java instance initializer block. */
    JAVA_INSTANCE_INITIALIZER(true, true, false),
    /** Java lambda expression with executable behavior. */
    JAVA_LAMBDA(true, true, false),
    /** Java field initializer with executable evaluation. */
    JAVA_FIELD_INITIALIZER(true, true, false),
    /** Java {@code native} declaration that crosses a native ABI boundary. */
    JAVA_NATIVE_DECLARATION(false, true, true),
    /** JNI-exported native entry point. */
    JNI_EXPORT(true, false, true),
    /** C function definition. */
    C_FUNCTION(true, false, true),
    /** C++ function or method definition. */
    CPP_FUNCTION(true, false, true),
    /** HotSpot architecture-description rule. */
    HOTSPOT_AD_RULE(true, false, true),
    /** Architecture-specific assembly routine. */
    ASSEMBLY_ROUTINE(true, false, true),
    /** Build-system rule that affects produced artifacts. */
    BUILD_RULE(true, false, false),
    /** Executable function in a repository script. */
    SCRIPT_FUNCTION(true, false, false),
    /** Resource or generated-data declaration that affects the product. */
    RESOURCE_DECLARATION(false, false, false),
    /** Non-executable declaration that carries a semantic contract. */
    DECLARATION_ONLY(false, false, false);

    private final boolean executable;
    private final boolean javaAtom;
    private final boolean nativeAtom;

    M3AtomKind(boolean executable, boolean javaAtom, boolean nativeAtom) {
        this.executable = executable;
        this.javaAtom = javaAtom;
        this.nativeAtom = nativeAtom;
    }

    /**
     * Returns whether the atom contains executable behavior.
     *
     * @return {@code true} for executable atoms
     */
    public boolean executable() {
        return executable;
    }

    /**
     * Returns whether javac/JVM product gates apply.
     *
     * @return {@code true} for Java atoms
     */
    public boolean javaAtom() {
        return javaAtom;
    }

    /**
     * Returns whether native compiler, ABI and runtime gates apply.
     *
     * @return {@code true} for native-boundary atoms
     */
    public boolean nativeAtom() {
        return nativeAtom;
    }
}
