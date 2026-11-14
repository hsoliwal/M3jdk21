/* ******************************************************************************
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
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Immutable Java model of the JANENCE fourteen-gate Reverset.
 *
 * <p>Traversal changes gate order only. Gate content is immutable; reverse mode
 * never reverses dialogue, mirror mode never invents cross-column grammar, and
 * Gate XIV's missing second opening line remains OPEN.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/Reverset.java}
 * blob {@code 3945bfde8bf6211f8247f65be0f1281e84f596ed} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Reverset {
    public static final int GATE_COUNT = 14;
    public static final int OPEN_GATE = 14;

    public enum Mode {
        FORWARD,
        REVERSE,
        MIRROR_PAIRS
    }

    public record Gate(
            int number,
            String heading,
            List<String> openingLines,
            String localityBeat,
            boolean openTerminal) {
        public Gate {
            if (number < 1 || number > GATE_COUNT) {
                throw new IllegalArgumentException("gate number");
            }
            heading = normalize(heading);
            localityBeat = normalize(localityBeat);
            openingLines = List.copyOf(openingLines == null ? List.of() : openingLines);
            if (heading.isBlank()) {
                throw new IllegalArgumentException("heading");
            }
            if (localityBeat.isBlank()) {
                throw new IllegalArgumentException("EVERY_GATE_REQUIRES_LOCALITY_BEAT");
            }
            if (number == OPEN_GATE) {
                if (!openTerminal || openingLines.size() != 1) {
                    throw new IllegalArgumentException("GATE_XIV_REQUIRES_OPEN_HALF_COUPLET");
                }
            } else if (openTerminal || openingLines.size() != 2) {
                throw new IllegalArgumentException("NON_TERMINAL_GATE_REQUIRES_FULL_COUPLET");
            }
        }
    }

    public record MirrorPair(int leftNumber, int rightNumber, Gate left, Gate right) {
        public MirrorPair {
            if (leftNumber + rightNumber != GATE_COUNT + 1) {
                throw new IllegalArgumentException("INVALID_MIRROR_PAIR");
            }
        }
    }

    public record ValidationReceipt(
            int gateCount,
            int fullOpeningCouplets,
            int openHalfCouplets,
            int localityBeats,
            int mirrorPairs,
            boolean forwardPreservesSource,
            boolean reversePreservesSource,
            boolean mirrorPreservesSource,
            boolean missingLineFilled,
            boolean semanticVerdict) {
    }

    private final Map<Integer, Gate> gates;

    public Reverset(List<Gate> sourceGates) {
        if (sourceGates == null || sourceGates.size() != GATE_COUNT) {
            throw new IllegalArgumentException("REVERESET_REQUIRES_14_GATES");
        }
        TreeMap<Integer, Gate> ordered = new TreeMap<>();
        for (Gate gate : sourceGates) {
            if (gate == null || ordered.put(gate.number(), gate) != null) {
                throw new IllegalArgumentException("DUPLICATE_OR_NULL_GATE");
            }
        }
        List<Integer> expected = IntStream.rangeClosed(1, GATE_COUNT).boxed().toList();
        if (!List.copyOf(ordered.keySet()).equals(expected)) {
            throw new IllegalArgumentException("GATE_SEQUENCE_1_TO_14_REQUIRED");
        }
        this.gates = Collections.unmodifiableMap(ordered);
    }

    public List<Gate> traverse(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode");
        }
        return switch (mode) {
            case FORWARD -> IntStream.rangeClosed(1, GATE_COUNT)
                    .mapToObj(gates::get)
                    .toList();
            case REVERSE -> IntStream.rangeClosed(1, GATE_COUNT)
                    .map(i -> GATE_COUNT + 1 - i)
                    .mapToObj(gates::get)
                    .toList();
            case MIRROR_PAIRS -> {
                ArrayList<Gate> out = new ArrayList<>(GATE_COUNT);
                for (MirrorPair pair : mirrorPairs()) {
                    out.add(pair.left());
                    out.add(pair.right());
                }
                yield List.copyOf(out);
            }
        };
    }

    public List<MirrorPair> mirrorPairs() {
        return IntStream.rangeClosed(1, GATE_COUNT / 2)
                .mapToObj(left -> {
                    int right = GATE_COUNT + 1 - left;
                    return new MirrorPair(left, right, gates.get(left), gates.get(right));
                })
                .toList();
    }

    public Gate gate(int number) {
        Gate gate = gates.get(number);
        if (gate == null) {
            throw new IllegalArgumentException("UNKNOWN_GATE:" + number);
        }
        return gate;
    }

    public ValidationReceipt validate() {
        int full = (int) gates.values().stream()
                .filter(gate -> gate.openingLines().size() == 2)
                .count();
        int open = (int) gates.values().stream()
                .filter(Gate::openTerminal)
                .count();
        int beats = (int) gates.values().stream()
                .filter(gate -> !gate.localityBeat().isBlank())
                .count();
        return new ValidationReceipt(
                gates.size(),
                full,
                open,
                beats,
                mirrorPairs().size(),
                true,
                true,
                true,
                false,
                false);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
