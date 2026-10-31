/*
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * One JUnit test per method atom of every owned decorator: for each TARGETS.tsv row the seeded
 * differential runs once against the Commons oracle, and each public method of the owned class
 * becomes a test that passes only when the seeded steps called it at least once and no mismatch
 * names it. A class contract test covers the factory, identity, serialization and backing-state
 * checks that belong to no single method.
 *
 * <p>The decorator-fill directory is the system property {@code m3.decorator.dir} (default: the
 * path from the repository root).
 */
final class M3DecoratorAtomTest {
    /** TARGETS.tsv columns: owned class, oracle, backing, factory, probe, policy. */
    static final int COL_OWNED = 0;
    static final int COL_ORACLE = 1;
    static final int COL_BACKING = 2;
    static final int COL_FACTORY = 3;
    static final int COL_POLICY = 5;
    /** One header row precedes the targets. */
    static final int HEADER_ROWS = 1;
    static final String TAB = "\t";
    static final String SPACE = " ";
    /** The policy argument that selects the harness default. */
    static final String DEFAULT_POLICY = "-";
    static final String PROP_DIR = "m3.decorator.dir";
    static final String DEFAULT_DIR = "m3/collections/qualification/decorator-fill-20261030";
    static final String TARGETS = "TARGETS.tsv";
    static final String CONTRACT = "class contract";
    static final String NEVER_EXERCISED = "never exercised by the seeded steps";
    static final Path DIR = Path.of(System.getProperty(PROP_DIR, DEFAULT_DIR));

    @TestFactory
    Stream<DynamicNode> everyMethodAtomOfEveryTarget() throws IOException {
        return targets(DIR).stream().map(M3DecoratorAtomTest::target);
    }

    /** The TARGETS.tsv rows of a decorator-fill directory, split into columns. */
    static List<String[]> targets(Path dir) throws IOException {
        return Files.readAllLines(dir.resolve(TARGETS), StandardCharsets.UTF_8).stream()
                .skip(HEADER_ROWS).filter(l -> !l.isBlank()).map(l -> l.split(TAB, -1)).toList();
    }

    /** The policy column of a target row, or the harness default. */
    static String policy(String[] t) {
        return t.length > COL_POLICY && !t[COL_POLICY].isBlank() ? t[COL_POLICY].strip()
                : DEFAULT_POLICY;
    }

    /** The display label of a target row: simple class name, backing and policy. */
    static String label(String[] t) {
        String owned = t[COL_OWNED];
        return owned.substring(owned.lastIndexOf('.') + 1) + SPACE + t[COL_BACKING] + SPACE
                + policy(t);
    }

    private static DynamicNode target(String[] t) {
        M3DecoratorDifferential.Result r;
        try {
            r = M3DecoratorDifferential.run(t[COL_OWNED], t[COL_ORACLE], t[COL_BACKING],
                    t[COL_FACTORY], policy(t));
        } catch (ReflectiveOperationException e) {
            return DynamicTest.dynamicTest(label(t), () -> {
                throw e;
            });
        }
        Stream<DynamicNode> atoms = r.exercised().entrySet().stream().map(e -> DynamicTest
                .dynamicTest(e.getKey(), () -> {
                    assertTrue(e.getValue() > 0, NEVER_EXERCISED);
                    assertEquals(List.of(), r.mismatches().stream()
                            .filter(m -> m.contains(SPACE + e.getKey() + TAB)).toList());
                }));
        DynamicNode contract = DynamicTest.dynamicTest(CONTRACT,
                () -> assertEquals(List.of(), r.mismatches()));
        return DynamicContainer.dynamicContainer(label(t),
                Stream.concat(atoms, Stream.of(contract)));
    }
}
