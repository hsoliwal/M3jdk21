// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import java.util.Arrays;
import java.util.List;

/** Standalone Java 21 qualification of stored source-fact differences. */
public final class M3FileSourceFactsDeltaContract {
    private static final int[] COMPONENT_FLAGS = {
        M3FileSourceFacts.SOURCE_URI_CHANGED,
        M3FileSourceFacts.SOURCE_CONTENT_CHANGED,
        M3FileSourceFacts.SOURCE_CONTENT_CHANGED,
        M3FileSourceFacts.COMPILATION_CONTEXT_CHANGED,
        M3FileSourceFacts.PROVIDER_CHANGED,
        M3FileSourceFacts.SCHEMA_CHANGED,
        M3FileSourceFacts.PHASE_CHANGED
    };
    private static int checks;

    private M3FileSourceFactsDeltaContract() {}

    public static void main(String[] arguments) {
        recordShapeAndFlagValues();
        equalValuesAndNullBaseline();
        eachIdentityComponent();
        allComponentCombinations();
        changedClasspathOrder();
        sourceChangesDoNotImplyAbiClassification();
        digestProtocolVectors();
        capturedMutableSourceIsStable();
        System.out.println("M3FileSourceFactsDeltaContract PASS checks=" + checks
                + " combinations=128 orderedPairs=16384");
    }

    private static void recordShapeAndFlagValues() {
        List<String> components = Arrays.stream(M3FileSourceFacts.class.getRecordComponents())
                .map(component -> component.getName() + ":" + component.getType().getName())
                .toList();
        require(components.equals(List.of(
                "sourceUri:java.lang.String", "sourceSha256:java.lang.String", "utf16Length:int",
                "contextSha256:java.lang.String", "providerId:java.lang.String", "schemaVersion:int",
                "phase:java.lang.String")), "record components changed");
        int[] flags = {
            M3FileSourceFacts.SOURCE_URI_CHANGED, M3FileSourceFacts.SOURCE_CONTENT_CHANGED,
            M3FileSourceFacts.COMPILATION_CONTEXT_CHANGED, M3FileSourceFacts.PROVIDER_CHANGED,
            M3FileSourceFacts.SCHEMA_CHANGED, M3FileSourceFacts.PHASE_CHANGED
        };
        for (int bit = 0; bit < flags.length; bit++) {
            equal(1 << bit, flags[bit], "public flag value " + bit);
        }
    }

    private static void equalValuesAndNullBaseline() {
        M3FileSourceFacts previous = variant(0);
        M3FileSourceFacts equalCopy = new M3FileSourceFacts(
                new String(previous.sourceUri()), new String(previous.sourceSha256()), previous.utf16Length(),
                new String(previous.contextSha256()), new String(previous.providerId()), previous.schemaVersion(),
                new String(previous.phase()));
        require(previous != equalCopy, "fixture must use separate values");
        equal(0, previous.changesFrom(previous), "same instance");
        equal(0, equalCopy.changesFrom(previous), "equal reconstructed record");
        require(previous.equals(equalCopy), "record equality remains unchanged");
        equal(previous.hashCode(), equalCopy.hashCode(), "record hash remains unchanged");
        try {
            previous.changesFrom(null);
            throw new AssertionError("null baseline was accepted");
        } catch (NullPointerException expected) {
            require("previous".equals(expected.getMessage()), "null identifies previous snapshot");
        }
    }

    private static void eachIdentityComponent() {
        M3FileSourceFacts previous = variant(0);
        for (int component = 0; component < COMPONENT_FLAGS.length; component++) {
            M3FileSourceFacts changed = variant(1 << component);
            equal(COMPONENT_FLAGS[component], changed.changesFrom(previous), "component " + component);
            equal(COMPONENT_FLAGS[component], previous.changesFrom(changed), "reverse component " + component);
        }
        // Metadata can be constructed directly: length disagreement must not be hidden by equal hashes.
        M3FileSourceFacts lengthOnly = variant(1 << 2);
        require(previous.sourceSha256().equals(lengthOnly.sourceSha256()), "equal digest fixture");
        equal(M3FileSourceFacts.SOURCE_CONTENT_CHANGED, lengthOnly.changesFrom(previous), "length alone");
    }

    private static void allComponentCombinations() {
        M3FileSourceFacts[] variants = new M3FileSourceFacts[1 << COMPONENT_FLAGS.length];
        for (int combination = 0; combination < variants.length; combination++) {
            variants[combination] = variant(combination);
        }
        for (int current = 0; current < variants.length; current++) {
            for (int previous = 0; previous < variants.length; previous++) {
                int observed = variants[current].changesFrom(variants[previous]);
                int expected = flagMask(current ^ previous);
                equal(expected, observed, "combination pair " + current + "/" + previous);
                require((observed == 0) == variants[current].equals(variants[previous]),
                        "zero must match complete record identity");
            }
        }
    }

    private static void changedClasspathOrder() {
        String source = "class Example {}";
        M3FileSourceFacts previous = capture(source,
                List.of("javac=21", "classpath", "a.jar", "b.jar"));
        M3FileSourceFacts reordered = capture(source,
                List.of("javac=21", "classpath", "b.jar", "a.jar"));
        require(previous.sourceSha256().equals(reordered.sourceSha256()), "source stays identical");
        equal(M3FileSourceFacts.COMPILATION_CONTEXT_CHANGED,
                reordered.changesFrom(previous), "classpath order changes context only");
        M3FileSourceFacts sameOrder = capture(source,
                List.of("javac=21", "classpath", "a.jar", "b.jar"));
        equal(0, sameOrder.changesFrom(previous), "same ordered compilation context");
    }

    private static void sourceChangesDoNotImplyAbiClassification() {
        String original = "class Example { static final int VALUE = 1; int value() { return 1; } }";
        List<String> context = List.of("javac=21", "--release=21", "-proc:none");
        M3FileSourceFacts previous = capture(original, context);
        String[] changes = {
            original.replace("VALUE = 1", "VALUE = 2"),
            original.replace("return 1", "return 2"),
            original + " // documentation"
        };
        for (String changed : changes) {
            M3FileSourceFacts current = capture(changed, context);
            equal(M3FileSourceFacts.SOURCE_CONTENT_CHANGED,
                    current.changesFrom(previous), "content without guessed ABI classification");
        }
        equal(previous.utf16Length(), capture(changes[0], context).utf16Length(), "same-length constant change");
        equal(previous.utf16Length(), capture(changes[1], context).utf16Length(), "same-length body change");
    }

    private static void digestProtocolVectors() {
        require(M3FileSourceFacts.sourceSha256("").equals(
                "682d087c89bab38303a5e9be7b30808459818b069fca4a1b4cbc061e92aee961"),
                "empty-source protocol vector");
        String unusual = new String(new char[] {0, 'A', (char) 0xD800, 'B', (char) 0xDC00});
        String unusualDigest = "a33752cb59e41316d5b74aceebe0009a2bc55a4f32f8524c25bb7fd7aba54d26";
        require(M3FileSourceFacts.sourceSha256(unusual).equals(unusualDigest),
                "UTF-16 protocol preserves unpaired surrogates");
        require(M3FileSourceFacts.sourceSha256ImmutableView(unusual).equals(unusualDigest),
                "immutable view preserves digest protocol");
        require(M3FileSourceFacts.contextSha256(List.of()).equals(
                "fceaf4acdfd3f6c0e5f5ee782432b26335fbb86adf54fbfe834c74f496fd25f5"),
                "empty-context protocol vector");
        require(M3FileSourceFacts.contextSha256(List.of("javac=21", "classpath", "a.jar", "b.jar")).equals(
                "985fdfe2235e32ff12ff8504a033a2be80727ed623fed1047dacb84f04fc002b"),
                "ordered-context protocol vector");
        require(!M3FileSourceFacts.contextSha256(List.of("ab", "c"))
                        .equals(M3FileSourceFacts.contextSha256(List.of("a", "bc"))),
                "context frames do not collapse boundaries");
    }

    private static void capturedMutableSourceIsStable() {
        StringBuilder source = new StringBuilder("class Example {}");
        String context = M3FileSourceFacts.contextSha256(List.of("javac=21"));
        M3FileSourceFacts captured = M3FileSourceFacts.capture(
                "file:///Example.java", source, context, "javac", 1, "PARSE");
        M3FileSourceFacts immutable = M3FileSourceFacts.capture(
                "file:///Example.java", source.toString(), context, "javac", 1, "PARSE");
        source.append(" // later edit");
        equal(0, captured.changesFrom(immutable), "captured snapshot survives caller mutation");
        M3FileSourceFacts later = M3FileSourceFacts.capture(
                "file:///Example.java", source, context, "javac", 1, "PARSE");
        equal(M3FileSourceFacts.SOURCE_CONTENT_CHANGED,
                later.changesFrom(captured), "new capture identifies later mutation");
    }

    private static M3FileSourceFacts capture(String source, List<String> context) {
        return M3FileSourceFacts.capture("file:///Example.java", source,
                M3FileSourceFacts.contextSha256(context), "javac", 1, "PARSE");
    }

    private static M3FileSourceFacts variant(int combination) {
        return new M3FileSourceFacts(
                selected(combination, 0) ? "file:///B.java" : "file:///A.java",
                (selected(combination, 1) ? "1" : "0").repeat(64),
                selected(combination, 2) ? 2 : 1,
                (selected(combination, 3) ? "3" : "2").repeat(64),
                selected(combination, 4) ? "ecj" : "javac",
                selected(combination, 5) ? 2 : 1,
                selected(combination, 6) ? "ANALYZE" : "PARSE");
    }

    private static boolean selected(int combination, int component) {
        return (combination & (1 << component)) != 0;
    }

    private static int flagMask(int componentChanges) {
        int expected = 0;
        for (int component = 0; component < COMPONENT_FLAGS.length; component++) {
            if (selected(componentChanges, component)) expected |= COMPONENT_FLAGS[component];
        }
        return expected;
    }

    private static void equal(int expected, int actual, String description) {
        require(expected == actual, description + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
}
