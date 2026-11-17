/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package jdk.internal.m3.paradigm.contract;

/**
 * Composite opt-in arithmetic capability for three established Java hooks.
 *
 * <p>Each operation has its own independent capability:
 * {@link IAdditive}, {@link ISubtractive}, and {@link IMultiplicative}.
 * Implementing this composite merely joins the three already-supported
 * operations; it does not create new arithmetic semantics. Partial providers
 * should implement only their relevant interfaces. Separate {@link IDivisible}
 * and {@link IRemainder} interfaces are opt-in only for types that already
 * define division or remainder.
 *
 * <p>The final {@link ComplexNumber} record already defines the three hooks.
 * Unrelated final classes can instead supply a separate adapter; no superclass
 * insertion is required. This interface does <em>not</em> make Java operators
 * overloadable. A future M3 compiler must gate symbolic lowering behind an
 * explicitly selected source mode and independent type-resolution proofs.
 *
 * @param <T> declared operand and result type
 */
public interface IArithmeticDefaults<T>
        extends IAdditive<T>, ISubtractive<T>, IMultiplicative<T> { }
