<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->

# M3 semantic census — anonymous source-owner compatibility

On Java 21, anonymous classes may have static methods. Javac provides an
empty simple name for these classes, so the existing legacy census type
label is `Anonymous.`. It must remain unchanged in the default CLI output.

The additional opt-in semantic source ledger uses a separate type stack
with deterministic `Anonymous.<anonymous1>` labeling. Both projections
come from one JavacTask parse and one structural source traversal.
No compiler API, public JDK API, JNI ABI, atom kind or table header changes.

The canonical source-sealed Synexia successor
`m3jdk21-semantic-census-anonymous-20261101` updates exactly two
already-versioned Java source atoms. The original v1 recipe remains intact.

Executed on stock JDK21: strict compilation, 154/154 dependency-free
checks, real original vs revised anonymous-class legacy TSV byte parity,
exact SHA256 source spans, original malformed parse RED versus refusal
GREEN, and repeated CLI fixed point. The full Maven/OpenRewrite SDK,
Junit, source-built M3JDK21 product, JNI/HotSpot/jtreg and Windows/macOS
are explicitly NOT_RUN.
