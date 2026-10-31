/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * One JUnit test per atom of the annotations convergence, from MAP_RULES.tsv:
 * <ul>
 * <li>CANONICAL: the jdk.m3 owner has the donor's targets, retention, elements and
 * meta-annotations (both compiled and on the class path);</li>
 * <li>MAP: the owner is an annotation type whose targets cover type use, where every mapped
 * donor annotation is placed after the consumer recipe.</li>
 * </ul>
 * System property {@code m3.annotations.rules}: the MAP_RULES.tsv path (default: the path from
 * the repository root).
 */
final class JdkM3AnnotationsTest {
    /** MAP_RULES.tsv columns: donor, decision, relation, owner, note. */
    static final int COL_DONOR = 0;
    static final int COL_DECISION = 1;
    static final int COL_OWNER = 3;
    static final String TAB = "\t";
    static final String COMMENT = "#";
    static final String HEADER = "donor_fqn" + TAB;
    static final String CANONICAL = "CANONICAL";
    static final String MAP = "MAP";
    static final String ARROW = " -> ";
    static final String PROP_RULES = "m3.annotations.rules";
    static final String DEFAULT_RULES = "m3/tooling/annotations-absorb/MAP_RULES.tsv";
    static final String NOT_ANNOTATION = "owner is not an annotation type: ";
    static final String NO_TYPE_USE = "owner does not target TYPE_USE or a declaration context: ";
    static final Path RULES = Path.of(System.getProperty(PROP_RULES, DEFAULT_RULES));

    @TestFactory
    Stream<DynamicTest> everyCanonicalAtomEqualsItsDonor() throws IOException {
        return rows(CANONICAL).map(r -> DynamicTest.dynamicTest(r[COL_DONOR] + ARROW
                + r[COL_OWNER], () -> {
                    Class<?> donor = Class.forName(r[COL_DONOR]);
                    Class<?> owner = Class.forName(r[COL_OWNER]);
                    assertArrayEquals(targets(donor), targets(owner));
                    assertEquals(retention(donor), retention(owner));
                    assertEquals(elements(donor), elements(owner));
                    assertEquals(meta(donor), meta(owner));
                }));
    }

    @TestFactory
    Stream<DynamicTest> everyMappedAtomHasATypeUseOwner() throws IOException {
        return rows(MAP).map(r -> DynamicTest.dynamicTest(r[COL_DONOR] + ARROW + r[COL_OWNER],
                () -> {
                    Class<?> owner = Class.forName(r[COL_OWNER]);
                    assertTrue(owner.isAnnotation(), NOT_ANNOTATION + owner);
                    List<ElementType> t = Arrays.asList(targets(owner));
                    assertTrue(t.contains(ElementType.TYPE_USE) || t.contains(ElementType.TYPE),
                            NO_TYPE_USE + owner);
                }));
    }

    private static Stream<String[]> rows(String decision) throws IOException {
        return Files.readAllLines(RULES, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.startsWith(COMMENT) && !l.startsWith(HEADER) && !l.isBlank())
                .map(l -> l.split(TAB, -1)).filter(c -> c[COL_DECISION].equals(decision));
    }

    private static ElementType[] targets(Class<?> c) {
        Target t = c.getAnnotation(Target.class);
        return t == null ? new ElementType[0]
                : Arrays.stream(t.value()).sorted().toArray(ElementType[]::new);
    }

    private static RetentionPolicy retention(Class<?> c) {
        Retention r = c.getAnnotation(Retention.class);
        return r == null ? RetentionPolicy.CLASS : r.value();
    }

    private static List<String> elements(Class<?> c) {
        return Arrays.stream(c.getDeclaredMethods()).filter(m -> !m.isSynthetic())
                .map(m -> m.getName() + m.getReturnType().getName()).sorted().toList();
    }

    private static List<String> meta(Class<?> c) {
        return Arrays.stream(c.getAnnotations()).map(Annotation::annotationType)
                .map(Class::getName).sorted().toList();
    }
}
