/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/**
 * <b>[JDK INTERNAL]</b>
 * JDK-owned M3 backing, coordinate and reusable precompute support.
 *
 * <p>The {@code jdk.internal.mindex} package is an implementation package of
 * {@code java.base}. It is intentionally not exported. Types in this package
 * are not Java SE API and must not be used as an application compatibility
 * surface.</p>
 *
 * <h2>Ownership boundary</h2>
 *
 * <p>M3JDK owns every runtime type in this package. Synexia is a donor and
 * convergence workspace only; no class in this package may depend on a
 * {@code com.synexia.*} runtime type.</p>
 *
 * <p>The VM-special canonical String representation remains package-private
 * under {@code java.lang}: an M3 String is a canonical owner plus one packed
 * coordinate. Classes that are inseparable from that VM layout may remain in
 * {@code java.lang}. Reusable backing, search, regex and precompute kernels
 * belong here once they can operate through JDK-owned contracts.</p>
 *
 * <h2>Precompute contract</h2>
 *
 * <p>Precompute is implementation-internal. Derived facts may reject
 * candidates, order work, or accelerate exact operations, but they never own a
 * second spelling of canonical text and never change Java semantics when
 * absent or evicted.</p>
 *
 * <p>String/search facts bind exact M3 owner and range coordinates. Numeric
 * identifiers from unrelated owners are not interchangeable. {@link M3TQ}
 * is an example: trigram facts are necessary-condition metadata and a positive
 * result still requires the authoritative verifier.</p>
 *
 * <h2>External implementation references</h2>
 *
 * <p>Abstract domains such as language, reasoning, search, graphs, automata,
 * logic and mathematics are not owned by a project merely because that project
 * implements them. Provenance and licensing attach to specific reviewed
 * implementation artifacts: exact source/blob revisions, concrete encodings,
 * generated tables, serialization formats, tests, and particular solver or
 * algorithm realizations.</p>
 *
 * <p>Specific RE2/J, Lucene, Synexia, or other implementation artifacts may
 * inform independently written JDK-internal kernels only after provenance,
 * licensing and semantic parity are recorded by the donor workspace. Donor
 * package names and public APIs do not define M3JDK class names or ABI.</p>
 *
 * <p>Knowledge/reasoning planes without a concrete JDK consumer are not String
 * precompute and must not be transplanted into {@code java.lang.String}.</p>
 *
 * @see M3StringBacking
 * @see M3MappedStringBacking
 * @see M3TQ
 */
package jdk.internal.mindex;
