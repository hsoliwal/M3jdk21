/*******************************************************************************
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
package jdk.internal.m3.collections.packed;

import java.util.Arrays;

/**
 * Mutable runtime state for a packed semantic machine.
 *
 * <p>Boolean/categorical facts are stored as bits in a primitive {@code long[]} and
 * continuous-ish signals are quantized into a primitive {@code byte[]} with values
 * interpreted as unsigned {@code 0..255}. No object is retained per fact or scalar.
 *
 * <p>The state itself contains no rules or meaning. Meaning belongs to the compatible
 * {@link PackedSemanticMachine}; indexes are the stable boundary between semantic names
 * and runtime storage.
 *
 * <p>This class is mutable and not thread-safe. A state should be owned by one worker
 * at a time unless external synchronization is supplied.
 *
 * @since 1.0
 * Runtime semantic state: active facts in {@code long[]} and quantized scalar channels
 * in {@code byte[]}. No object is retained per fact or scalar.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/semantic/PackedSemanticState.java}
 * blob {@code 75360d86ba9518658583f1a1a119799273090171} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedSemanticState {
    private final int factCount;
    private final long[] active;
    private final byte[] scalars;

    /**
     * Creates an empty state.
     *
     * @param factCount number of fact bits; must be positive
     * @param scalarCount number of unsigned-byte scalar channels; may be zero
     * @throws IllegalArgumentException if the dimensions are invalid
     */
    public PackedSemanticState(int factCount, int scalarCount) {
        if (factCount <= 0) {
            throw new IllegalArgumentException("factCount");
        }
        if (scalarCount < 0) {
            throw new IllegalArgumentException("scalarCount");
        }
        this.factCount = factCount;
        this.active = new long[PackedSemanticMasks.wordCount(factCount)];
        this.scalars = new byte[scalarCount];
    }

    /**
     * Returns the logical number of fact bits.
     *
     * @return fact count
     */
    public int factCount() {
        return factCount;
    }

    /**
     * Returns the number of unsigned-byte scalar channels.
     *
     * @return scalar count
     */
    public int scalarCount() {
        return scalars.length;
    }

    /**
     * Returns the number of primitive long words backing fact state.
     *
     * @return {@code ceil(factCount / 64)}
     */
    public int wordCount() {
        return active.length;
    }

    /**
     * Returns primitive payload size, excluding Java object/array headers.
     *
     * @return {@code wordCount() * 8 + scalarCount()} bytes
     */
    public long payloadBytes() {
        return (long) active.length * Long.BYTES + scalars.length;
    }

    /**
     * Tests a fact bit.
     *
     * @param fact fact index
     * @return true when active
     * @throws IndexOutOfBoundsException if the fact index is invalid
     */
    public boolean active(int fact) {
        checkFact(fact);
        return PackedSemanticMasks.get(active, fact);
    }

    /**
     * Directly activates a fact bit.
     *
     * <p>This method bypasses machine guards/ownership rules and is intended for state
     * initialization/deserialization. Normal semantic transitions should use
     * {@link PackedSemanticMachine#activate(PackedSemanticState, int)}.
     *
     * @param fact fact index
     */
    public void activate(int fact) {
        checkFact(fact);
        PackedSemanticMasks.set(active, fact);
    }

    /**
     * Directly clears a fact bit.
     *
     * <p>This method bypasses machine guards/ownership rules and is intended for state
     * initialization/deserialization. Normal semantic transitions should use
     * {@link PackedSemanticMachine#deactivate(PackedSemanticState, int)}.
     *
     * @param fact fact index
     */
    public void deactivate(int fact) {
        checkFact(fact);
        PackedSemanticMasks.clear(active, fact);
    }

    /**
     * Counts active fact bits.
     *
     * @return active-fact count
     */
    public int activeCount() {
        return PackedSemanticMasks.cardinality(active);
    }

    /**
     * Clears all fact bits while preserving scalar channels.
     */
    public void clearFacts() {
        Arrays.fill(active, 0L);
    }

    /**
     * Reads one scalar as an unsigned integer.
     *
     * @param scalar scalar index
     * @return scalar value in {@code 0..255}
     */
    public int scalarUnsigned(int scalar) {
        checkScalar(scalar);
        return PackedU8.unsigned(scalars[scalar]);
    }

    /**
     * Writes one scalar from an unsigned integer.
     *
     * <p>This direct setter does not recompute derived semantic facts. Normal semantic
     * changes should use {@link PackedSemanticMachine#scalar(PackedSemanticState, int, int)}.
     *
     * @param scalar scalar index
     * @param value value in {@code 0..255}
     * @throws IllegalArgumentException if the value is outside {@code 0..255}
     */
    public void scalarUnsigned(int scalar, int value) {
        checkScalar(scalar);
        scalars[scalar] = PackedU8.of(value);
    }

    /**
     * Clears all scalar channels while preserving fact bits.
     */
    public void clearScalars() {
        Arrays.fill(scalars, (byte) 0);
    }

    /**
     * Clears all fact bits and scalar channels.
     */
    public void clear() {
        clearFacts();
        clearScalars();
    }

    /**
     * Copies all fact/scalar data from another state of the same shape.
     *
     * @param source source state
     * @throws IllegalArgumentException if dimensions differ
     */
    public void copyFrom(PackedSemanticState source) {
        if (source.factCount != factCount || source.scalars.length != scalars.length) {
            throw new IllegalArgumentException("semantic state shape mismatch");
        }
        System.arraycopy(source.active, 0, active, 0, active.length);
        System.arraycopy(source.scalars, 0, scalars, 0, scalars.length);
    }

    /**
     * Returns a boundary copy of active fact words.
     *
     * @return newly allocated primitive word copy
     */
    public long[] activeWordsCopy() {
        return Arrays.copyOf(active, active.length);
    }

    /**
     * Returns a boundary copy of retained scalar bytes.
     *
     * @return newly allocated primitive byte copy
     */
    public byte[] scalarBytesCopy() {
        return Arrays.copyOf(scalars, scalars.length);
    }

    long[] activeWords() {
        return active;
    }

    byte[] scalarBytes() {
        return scalars;
    }

    private void checkFact(int fact) {
        if (fact < 0 || fact >= factCount) {
            throw new IndexOutOfBoundsException("fact=" + fact + ", factCount=" + factCount);
        }
    }

    private void checkScalar(int scalar) {
        if (scalar < 0 || scalar >= scalars.length) {
            throw new IndexOutOfBoundsException(
                    "scalar=" + scalar + ", scalarCount=" + scalars.length);
        }
    }
}
