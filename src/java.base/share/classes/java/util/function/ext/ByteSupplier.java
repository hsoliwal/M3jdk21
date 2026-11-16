/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

package java.util.function.ext;

/**
 * A supplier with the shape {@code () to byte}.
 *
 * <p>Primitive positions are primitive in the functional method. Reference arguments and results
 * may be null unless the implementation imposes a restriction. Invocations are caller-driven; this
 * interface implies no scheduling, caching, serialization, purity, resource ownership, or
 * thread-safety guarantee.
 *
 * This is a functional interface whose functional method is {@code getAsByte}.
 *
 * @since M3JDK 21
 */
@FunctionalInterface
public interface ByteSupplier {
    /**
     * Obtains a result from this supplier.
     *
     * @return the result
     */
    byte getAsByte();
}
