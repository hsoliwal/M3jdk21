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

import jdk.internal.m3.collections.TokenInterner;
import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.util.UUID;

/**
 * IPointer - Unified Variable-Length Context Address.
 *
 * <h2>From TLOUOOIACI Property #1</h2>
 * <blockquote>
 * "X Spine = Immutable - Never mutates, append-only, variable-length pointers"
 * </blockquote>
 *
 * <h2>Axiom #9</h2>
 * <blockquote>
 * "Pointers unify all constructs"
 * </blockquote>
 *
 * <h2>Two Addressing Modes</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │  MODE          │ FORMAT        │ USE CASE                                   │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │  Sequence      │ ux{hex}       │ Fast runtime addressing (O(1) lookup)      │
 * │  Identity      │ UUID          │ Durable identity (SQLite filename)         │
 * └─────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Pointer Properties</h2>
 * <ul>
 *   <li>Variable-length (not fixed-width) - critical for infinite space</li>
 *   <li>No collisions - unique identity guaranteed</li>
 *   <li>No hashing needed - direct dereference</li>
 *   <li>O(1) resolution - constant-time lookup</li>
 *   <li>Hardware-friendly - aligned to CPU operations</li>
 * </ul>
 *
 * @param <Self> Self-type for fluent chaining
 * @since ContextOS 1.0 - Mandelbrot Addressing
 */
@ISpec(
    value = "Unified Variable-Length Context Address - sealed union of Sequence and Identity pointers",
    constraints = {
        "SequencePointer: ux{hex} format for O(1) runtime addressing",
        "IdentityPointer: UUID format for durable persistence",
        "Variable-length, no collisions, no hashing needed",
        "Pointers unify all constructs (Axiom #9)"
    },
    invariants = {
        "isRoot() returns true iff value==0 or id==null",
        "toCanonical() returns parseable string representation",
        "parse(toCanonical()) returns equivalent pointer"
    },
    examples = {
        "IPointer.seq(42)",
        "IPointer.id(uuid)",
        "IPointer.parse(\"ux1a\")"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/mandelbrot/IPointer.java}
 * blob {@code 213a1370bd0854224240d77ce6983ec1fbdf29ec} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface IPointer<Self extends IPointer<Self>>
        extends Comparable<IPointer<?>>
        permits IPointer.SequencePointer, IPointer.IdentityPointer {

    /** Root pointer sequence value. */
    long ROOT_VALUE = 0L;

    /** Prefix for sequence string representation. */
    String PREFIX = "ux";

    // --- COMMON OPERATIONS ---

    /**
     * Check if this is the root pointer.
     *
     * @return true if root
     */
    boolean isRoot();

    /**
     * Check if this pointer is valid.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Get canonical string representation.
     *
     * @return canonical string
     */
    String toCanonical();

    /**
     * Create next pointer in sequence.
     *
     * @return next pointer
     */
    Self next();

    /**
     * Get sequence value (for SequencePointer) or -1 (for IdentityPointer).
     *
     * @return sequence value or -1
     */
    default long value() {
        return -1L;
    }

    // --- SEQUENCE POINTER (ux format) ---

    /**
     * Sequence Pointer - Fast runtime addressing using long values.
     *
     * <p>Format: ux{hex_value} (e.g., ux0, ux1a, uxfff)
     */
    record SequencePointer(long value) implements IPointer<SequencePointer> {

        /** Singleton root. */
        public static final SequencePointer ROOT = new SequencePointer(ROOT_VALUE);

        @Override
        public boolean isRoot() {
            return value == ROOT_VALUE;
        }

        @Override
        public boolean isValid() {
            return value >= 0;
        }

        @Override
        public String toCanonical() {
            return PREFIX + Long.toHexString(value);
        }

        @Override
        public SequencePointer next() {
            return new SequencePointer(value + 1);
        }

        /**
         * Add offset to pointer.
         *
         * @param offset the offset
         * @return new pointer
         */
        public SequencePointer add(long offset) {
            return new SequencePointer(value + offset);
        }

        /**
         * Calculate distance to another pointer.
         *
         * @param other other pointer
         * @return distance
         */
        public long distanceTo(SequencePointer other) {
            return other.value - value;
        }

        @Override
        public int compareTo(IPointer<?> other) {
            if (other instanceof SequencePointer sp) {
                return Long.compare(value, sp.value);
            }
            return toCanonical().compareTo(other.toCanonical());
        }

        @Override
        public String toString() {
            return toCanonical();
        }
    }

    // --- IDENTITY POINTER (UUID format) ---

    /**
     * Identity Pointer - Durable addressing using UUID.
     *
     * <p>Used for: SQLite filenames, cross-session references, persistence.
     */
    record IdentityPointer(UUID id) implements IPointer<IdentityPointer> {

        /** Null identity. */
        public static final IdentityPointer NULL = new IdentityPointer((UUID) null);

        @Override
        public boolean isRoot() {
            return id == null;
        }

        @Override
        public boolean isValid() {
            return id != null;
        }

        @Override
        public String toCanonical() {
            return id != null ? id.toString() : "null";
        }

        @Override
        public IdentityPointer next() {
            return new IdentityPointer(UUID.randomUUID());
        }

        /**
         * Get short ID (first 8 chars).
         *
         * @return short ID
         */
        public String shortId() {
            return id != null ? id.toString().substring(0, 8) : "null";
        }

        @Override
        public int compareTo(IPointer<?> other) {
            if (other instanceof IdentityPointer ip) {
                if (id == null && ip.id == null) return 0;
                if (id == null) return -1;
                if (ip.id == null) return 1;
                return id.compareTo(ip.id);
            }
            return toCanonical().compareTo(other.toCanonical());
        }

        @Override
        public String toString() {
            return toCanonical();
        }
    }

    // --- FACTORY METHODS ---

    /**
     * Create sequence pointer from long value.
     *
     * @param value pointer value
     * @return sequence pointer
     */
    static SequencePointer seq(long value) {
        return new SequencePointer(value);
    }

    /**
     * Create sequence pointer from long value.
     * Alias for {@link #seq(long)}.
     *
     * @param value pointer value
     * @return sequence pointer
     */
    static SequencePointer of(long value) {
        return seq(value);
    }

    /**
     * Create root sequence pointer.
     *
     * @return root pointer
     */
    static SequencePointer root() {
        return SequencePointer.ROOT;
    }

    /**
     * Create identity pointer from UUID.
     *
     * @param id the UUID
     * @return identity pointer
     */
    static IdentityPointer id(UUID id) {
        return new IdentityPointer(id);
    }

    /**
     * Create new random identity pointer.
     *
     * @return new identity pointer
     */
    static IdentityPointer newId() {
        return new IdentityPointer(UUID.randomUUID());
    }

    /**
     * Create null identity pointer.
     *
     * @return null pointer
     */
    static IdentityPointer nullId() {
        return IdentityPointer.NULL;
    }

    /**
     * Parse pointer from string.
     *
     * <p>Accepts: "ux1a" (sequence), UUID string (identity).
     *
     * @param s the string
     * @return parsed pointer
     */
    static IPointer<?> parse(String s) {
        if (s == null || s.isBlank()) {
            return IdentityPointer.NULL;
        }
        String normalized = s.trim().toLowerCase();
        if (normalized.startsWith(PREFIX)) {
            long value = Long.parseLong(normalized.substring(PREFIX.length()), 16);
            return new SequencePointer(value);
        }
        try {
            return new IdentityPointer(UUID.fromString(s.trim()));
        } catch (IllegalArgumentException e) {
            return new SequencePointer(Long.parseLong(normalized, 16));
        }
    }
}
