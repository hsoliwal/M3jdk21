<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3AST Facts.first performance admission — public product receiver

A **qualification-only** receiver of the canonical Synexia Maven recipe crate
in [Synexia PR #17115](https://github.com/hsoliwal/com.synexia/pull/17115).
This receiver adds no new runtime Java or JNI code and does not change any
public API. The prior source-changing recipes are already merged:
[Synexia #17055](https://github.com/hsoliwal/com.synexia/pull/17055),
[Synexia #17056](https://github.com/hsoliwal/com.synexia/pull/17056),
and product [M3JDK21 #2943](https://github.com/hsoliwal/M3jdk21/pull/2943).

The exact current owner remains
`m3/ast/src/main/java/jdk/m3/ast/SyntaxIndex.java`
Git blob `db14c75a439b3366517c2b344b41d8f4f7a390fc`,
paired with `SyntaxTree.java` Git blob
`05e399e63fb011630c7d38a9f7132f78526a1a6c`.
The original proof class Git blob is
`c995054c582991eefbbe995d9079292eb766cb97`,
copied **unchanged** from the first-party Synexia crate.

The benchmark verifies the original primitive-array scan and the actual
`Facts.first` return identical values before collecting measurements.
It uses five paired alternating samples of 20,000 calls; one-time cold
index construction and early/late/missing/mixed query patterns are
reported separately. Collision refusal with 256 equal-hash keys and
cold small / over-limit workloads are checked.

The **hard** admission tests are exact Java source identity, correct
earliest-match behavior, permanent fallback under adversarial collisions,
no optional cache for small or oversized inputs, and the 2 MiB maximum
`int[]` payload for an admitted cache. All `BENCH` timings are *advisory*;
there is no unjustified CI speedup threshold, and the experiment is not
a substitute for controlled JMH or a full product/JVM benchmark.

Run:
```sh
sh m3/ast/recipe/facts-first-performance-20261112/verify.sh
mvn -B -ntp -f m3/ast/recipe/facts-first-performance-20261112/pom.xml verify
```
The isolated Maven crate compiles **the actual product**
`SyntaxTree.java`, `SyntaxIndex.java`, and proof class, while avoiding
the `m3/ast/pom.xml` curated JUnit `5.10.2-m3-source-1` dependency.
That original module dependency is not altered; its reactor gate remains
separate.

At review creation this benchmark has **not executed** on product CI;
do not claim performance qualification until the dedicated Actions
workflow logs a PASS line and BENCH rows. Native JNI remains out of scope
for this scalar Java lookup unless a measured batched native workload
justifies ABI/GC and multi-platform integration.
