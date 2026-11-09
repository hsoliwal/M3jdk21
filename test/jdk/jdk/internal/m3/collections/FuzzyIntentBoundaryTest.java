/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.collections;

import jdk.m3.collections.PrimitiveAdaptiveCharMap;
import jdk.m3.collections.PrimitiveDirectByteMap;
import jdk.m3.collections.PrimitiveKind;
import jdk.m3.collections.PrimitiveSpscRingBuffer;
import jdk.m3.collections.PrimitiveTopK;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Retained category-selection counterexamples; no collection algorithm is replaced. */
final class FuzzyIntentBoundaryTest {
    private final FuzzyStructureFactory factory = new FuzzyStructureFactory();

    @Test
    void exactMapConnectorDoesNotBecomeTopKAndKindsStayInOrder() {
        FuzzyStructureFactory.Plan direct = factory.plan(" DENSE byte TO int map ");
        assertEquals("dense byte to int map", direct.normalizedIntent());
        assertEquals(FuzzyStructureFactory.Family.MAP, direct.family());
        assertEquals(PrimitiveKind.BYTE, direct.keyKind());
        assertEquals(PrimitiveKind.INT, direct.valueKind());
        assertInstanceOf(PrimitiveDirectByteMap.class, direct.create());
        assertInstanceOf(PrimitiveAdaptiveCharMap.class,
                factory.create("sparse char to byte map 200"));
        assertTrue(direct.rationale().stream().noneMatch(value -> value.contains("to->top")));
    }

    @Test
    void shortAndLongTypoToleranceAndActualTopKRemainAvailable() {
        assertEquals(FuzzyStructureFactory.Family.MAP, factory.plan("mp long long").family());
        assertEquals(FuzzyStructureFactory.Family.SET, factory.plan("st long").family());
        assertEquals(PrimitiveKind.INT, factory.plan("nt list").keyKind());
        FuzzyStructureFactory.Plan top = factory.plan("tp 7 largest long values");
        assertEquals(FuzzyStructureFactory.Family.TOP_K, top.family());
        assertEquals(7, top.limit());
        assertEquals(PrimitiveTopK.Mode.KEEP_LARGEST,
                assertInstanceOf(PrimitiveTopK.class, top.create()).mode());
        assertEquals(FuzzyStructureFactory.Storage.SORTED_GAP,
                factory.plan("soreted long set mutation hevy").storage());
        assertEquals(FuzzyStructureFactory.Storage.SORTED_ARRAY,
                factory.plan("sroted long set").storage());
        assertInstanceOf(PrimitiveSpscRingBuffer.class,
                factory.create("spsc int quue 32"));
        assertEquals(FuzzyStructureFactory.Family.TOP_K,
                factory.plan("top 100 smallest double values").family());
    }

    @TestFactory
    Stream<DynamicTest> roleCardinalitiesAreIndependentOfClauseOrderAndAliases() {
        List<DynamicTest> cases = new ArrayList<>();
        String[][] quantifiers = {{"one", "single"}, {"many", "multiple", "multi"}};
        for (int producers = 0; producers < 2; producers++) {
            for (int consumers = 0; consumers < 2; consumers++) {
                FuzzyStructureFactory.Concurrency expected = producers == 0
                        ? consumers == 0 ? FuzzyStructureFactory.Concurrency.SPSC
                                         : FuzzyStructureFactory.Concurrency.SPMC
                        : consumers == 0 ? FuzzyStructureFactory.Concurrency.MPSC
                                         : FuzzyStructureFactory.Concurrency.MPMC;
                for (String producerCount : quantifiers[producers]) {
                    for (String consumerCount : quantifiers[consumers]) {
                        for (String producerNoun : List.of("producer", "producers")) {
                            for (String consumerNoun : List.of("consumer", "consumers")) {
                                String first = producerCount + " " + producerNoun;
                                String second = consumerCount + " " + consumerNoun;
                                for (String roles : List.of(first + " " + second, second + " " + first)) {
                                    String intent = roles + " long queue 32";
                                    cases.add(DynamicTest.dynamicTest(intent, () -> {
                                        FuzzyStructureFactory.Plan plan = factory.plan(intent);
                                        assertEquals(expected, plan.concurrency());
                                        assertEquals(FuzzyStructureFactory.Family.QUEUE, plan.family());
                                        assertEquals(FuzzyStructureFactory.Storage.RING, plan.storage());
                                        assertEquals(32, plan.expectedSize());
                                        assertEquals(plan.implementationType(), plan.create().getClass().getName());
                                    }));
                                }
                            }
                        }
                    }
                }
            }
        }
        return cases.stream();
    }

    @Test
    void incompleteOrContradictoryRoleEvidenceDoesNotInventAMode() {
        for (String roles : List.of("many producers consumer", "producer many consumers",
                "one producer consumer", "one producer many producers one consumer",
                "many producers one producer one consumer", "one producer one consumer many consumers")) {
            assertEquals(FuzzyStructureFactory.Concurrency.NONE,
                    factory.plan(roles + " long queue").concurrency(), roles);
        }
        assertEquals(FuzzyStructureFactory.Concurrency.SPMC,
                factory.plan("one producer one producer many consumers long queue").concurrency());
    }

    @Test
    void explicitModesAndBlockingKeepTheirExistingPrecedence() {
        for (String mode : List.of("spsc", "mpsc", "spmc", "mpmc")) {
            FuzzyStructureFactory.Concurrency expected = FuzzyStructureFactory.Concurrency
                    .valueOf(mode.toUpperCase(java.util.Locale.ROOT));
            assertEquals(expected, factory.plan(mode + " many producers one consumer int queue").concurrency());
        }
        assertEquals(FuzzyStructureFactory.Concurrency.SPSC,
                factory.plan("spsc mpsc spmc mpmc int queue").concurrency());
        assertEquals(FuzzyStructureFactory.Concurrency.BLOCKING_MPMC,
                factory.plan("blocking spsc one producer one consumer int queue").concurrency());
    }

    @Test
    void nullBlankAndUnknownIntentsRetainTheirExceptions() {
        assertThrows(NullPointerException.class, () -> factory.plan(null));
        assertThrows(IllegalArgumentException.class, () -> factory.plan(" \t\n "));
        assertThrows(IllegalArgumentException.class, () -> factory.plan("to"));
        assertThrows(IllegalArgumentException.class, () -> factory.plan("unrecognizedword"));
    }

    @Test
    void equalDistanceMatchesUseDeclaredVocabularyOrderAcrossJvms() throws Exception {
        assertEquals("map long map", factory.plan("mip long map").normalizedIntent());
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path"));
        for (int repetition = 0; repetition < 5; repetition++) {
            Process process = new ProcessBuilder(java, "-cp", classpath, TieProbe.class.getName())
                    .redirectErrorStream(true).start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) process.destroyForcibly();
            assertTrue(completed, "independent JVM exceeded the bounded probe time");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            assertEquals(0, process.exitValue(), output);
            assertEquals("map long map", output);
        }
    }

    public static final class TieProbe {
        private TieProbe() { }
        public static void main(String[] args) {
            System.out.println(new FuzzyStructureFactory().plan("mip long map").normalizedIntent());
        }
    }
}
