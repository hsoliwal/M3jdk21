# Long row path masks — M3JDK21 receiver, 2026-10-18

**Status:** additive separately built M3 collection proving-ground slice, not java.util/JDK promotion.

## Capability and contract

`M3LongIndexPathMasks` specializes the existing first-party `M3IndexPathMasks` problem category for `long[]` payloads without allocating an `int[]` identity-index lane or boxed entries. It consumes caller-owned `long[]` encounter-order values, `int[]` logical-to-physical offsets, aligned `int[][]` state planes, per-plane required and forbidden 32-bit masks, and signed path strides. The two-pass exact output is detached and preserves duplicate values and encounter order; `scanWindow` writes a bounded primitive page into the caller's buffer without allocating additional arrays. It does not replace live filtered/sorted wrappers or maintain a cache across mutations. The caller must keep inputs stable during a call.

The existing `M3IndexPathMasks` remains the canonical JNI backend for integer row IDs and retains its current native ABI. This initial Java specialization has **no native backend**, no new JNI symbols, and no automatic native fallback. The user-level sorted factory can consume its detached output through `M3LongSortedSnapshot.fromSorted()` only after the source's signed unique ordering contract is satisfied.

## Recipe-first custody

Canonical recipe and original first-party templates are in `hsoliwal/com.synexia` on `m3/long-index-path-masks-20261018`, under `synexia-m3-recipe/recipes/m3-long-index-path-masks-20261018`. M3JDK21 mirrors the exact Maven recipe and SHA-256-sealed Java templates under the local `recipe/` subdirectory. Run the recipe against a fresh checkout, then check the generated source at the fixed point. The recipe preflights *both* targets and refuses modified existing files and symlinks. It is intentionally an exact absent-file source-admission recipe, not an alternative AST transformation framework. For changes to existing Java owners, use the established Synexia OpenRewrite recipe engine.

```sh
mvn -B -ntp -f m3/collections/qualification/m3-long-index-path-masks-20261018/recipe/pom.xml package
java -cp m3/collections/qualification/m3-long-index-path-masks-20261018/recipe/target/classes \
  com.synexia.m3.M3LongIndexPathMasksRecipe --check . \
  m3/collections/qualification/m3-long-index-path-masks-20261018/recipe/templates
javac --release 21 -proc:none -Xlint:all -Werror -d /tmp/m3-long-path \
  m3/collections/src/main/java/com/m3/collections/M3LongIndexPathMasks.java \
  m3/collections/src/test/java/com/m3/collections/M3LongIndexPathMasksSelfTest.java
java -cp /tmp/m3-long-path com.m3.collections.M3LongIndexPathMasksSelfTest
```

## Bounded evidence, provenance, and alternatives

Direct installed OpenJDK 21.0.12.1 `javac`/Java run: **PASS 3,022 assertions / 1,500 deterministic randomized cases**; source recipe apply/check/apply fixed point: PASS. No real Maven SDK run, full reactor, JNI extension, performance comparison, JDK image, or hosted CI is claimed by this local proof. The GitHub workflow runs source-custody and isolated Java gates on PR head.

Algorithm source is new first-party Apache-2.0 Java using the same lane/mask semantics as the existing `M3IndexPathMasks` owner. LeetCode, HackerRank and GeeksforGeeks are problem-category references only; no editorial or submission code is copied. `CATALOG.tsv` records candidate and contract decisions. The change does not modify preexisting APIs, `java.base`, JDK collections, existing JNI, or Synexia's append-only `develop` branch.
