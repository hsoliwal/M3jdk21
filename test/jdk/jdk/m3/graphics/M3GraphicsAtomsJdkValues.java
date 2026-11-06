/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Values of the JDK reference parameters the jdk.m3.graphics atom tests can build without natives (the generator's
 * kinds BOXED, SUPPLIER, RUNNABLE and URL): a boxed primitive from the primitive pools, a Supplier of a pool String, a
 * Runnable that does nothing, and a URL that is the oracle jar's manifest or an entry missing from that jar. Every
 * {@link #NULL_PERIOD}th round passes null instead. The objects are immutable and shared by both sides.
 */
final class M3GraphicsAtomsJdkValues {
    private static final int NULL_PERIOD = 8;
    private static final int NULL_PHASE = 7;
    private static final int URL_KINDS = 2;
    private static final int URL_PRESENT = 0;
    private static final String SUPPLIER = "java.util.function.Supplier";
    private static final String RUNNABLE = "java.lang.Runnable";
    private static final String URL_TYPE = "java.net.URL";
    private static final String MANIFEST = "META-INF/MANIFEST.MF";
    private static final String MISSING = "META-INF/M3_ATOM_TESTS_MISSING_ENTRY";
    private static final Map<String, Character> BOXED = Map.of("java.lang.Boolean", 'Z', "java.lang.Byte", 'B',
            "java.lang.Character", 'C', "java.lang.Short", 'S', "java.lang.Integer", 'I', "java.lang.Long", 'J',
            "java.lang.Float", 'F', "java.lang.Double", 'D');
    private static final Runnable NO_OP = () -> { };

    private M3GraphicsAtomsJdkValues() {}

    /** True for the JDK reference types this class gives values to. */
    static boolean handles(String binary) {
        return BOXED.containsKey(binary) || SUPPLIER.equals(binary) || RUNNABLE.equals(binary)
                || URL_TYPE.equals(binary);
    }

    /** The logical value of such a parameter for one round and position; empty for other types. */
    static Optional<M3GraphicsAtomsValues.Logical> value(String binary, long seed, int round, int position) {
        if (!handles(binary)) {
            return Optional.empty();
        }
        if (round % NULL_PERIOD == NULL_PHASE) {
            return Optional.of(M3GraphicsAtomsValues.ABSENT);
        }
        Character letter = BOXED.get(binary);
        Object v = letter != null ? M3GraphicsAtomsPools.primitive(letter, seed, round, position)
                : SUPPLIER.equals(binary) ? supplier(M3GraphicsAtomsPools.string(List.of(), seed, round, position))
                : RUNNABLE.equals(binary) ? NO_OP : url(round);
        return Optional.of(new M3GraphicsAtomsValues.Plain(v));
    }

    private static Supplier<String> supplier(String text) {
        return () -> text;
    }

    private static URL url(int round) {
        URL manifest = M3GraphicsAtomsOracle.oracleResource(MANIFEST)
                .orElseThrow(() -> new IllegalStateException(MANIFEST));
        if (round % URL_KINDS == URL_PRESENT) {
            return manifest;
        }
        try {
            return URI.create(manifest.toExternalForm().replace(MANIFEST, MISSING)).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException(MISSING, e);
        }
    }
}
