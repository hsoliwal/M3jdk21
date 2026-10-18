# M3 long-index path JNI — bounded Java/C17 receiver (2026-10-18)

**Phase:** additive, separately built `m3/collections` proof; not JDK `java.util` promotion, a JNI speedup claim, or a replacement for existing live filtered/sorted collection views.

## Canonical owners and contract

The merged first-party `M3LongIndexPathMasks` Java implementation (M3JDK21 PR #1250; Git blob `dd918fb13360408d71fc3577ee5c7bd255205340`) remains **unchanged**. This increment adds only a separate optional JNI backend `M3LongIndexPathMasksJNI`, a C17 Region implementation, and a differential test. The new native library is explicitly named `m3_collections_long_path_masks`; unavailable native requests throw `UnsatisfiedLinkError` with no Java fallback. Neither the existing int-path native ABI nor the Java source contract is modified.

Inputs are caller-borrowed `long[]` row payloads, `int[]` logical-to-physical offsets, multiple `int[][]` state planes, ordered required/forbidden `int[]` masks and a path `(first, count, stride)`. Signed strides, duplicates, negative and extreme `long` values, and caller-defined encounter order are preserved. Each mask stage requires `(state & required) == required && (state & forbidden) == 0`. The full selector returns a detached exact-length `long[]`; the bounded selector returns a packed next-ordinal/written-count token and writes into caller-owned output without a Java-side intermediate buffer. No comparator, predicate, class, object reference, raw heap address, or pinned Java pointer crosses or persists in JNI.

A 4,096-entry native stack staging array defers the bounded output write until complete prefix validation and a matching second-pass selection count. Any invalid inspected physical offset or inconsistent selection count is refused before modifying the output. The `MAX_WINDOW` limit mirrors the Java owner. Caller-side mutation of the borrowed input arrays during selection is outside both implementations' contracts; a source-generation/epoch owner must revalidate between windows. This is not a live JDK `NavigableSet` view.

## Recipe-first custody and reproducibility

Canonical source recipe: `hsoliwal/com.synexia`, `synexia-m3-recipe/recipes/m3-long-index-path-jni-20261018/`, developed separately from the public receiver. It **reuses** the prior source-sealed Java recipe mechanics, extends them to three absent Java/C source paths, and stores SHA-256 identities for each authoritative template. It verifies all template hashes and all occupied paths before writing, refuses symlinks and drift, is serial per file, and is idempotent. Source/test/native receiver files were emitted **by executing this recipe** in an isolated local checkout; no generated production files were edited by hand.

```sh
ROOT=m3/collections/qualification/m3-long-index-path-jni-20261018/recipe
mvn -B -ntp -f "$ROOT/pom.xml" package
java -cp "$ROOT/target/classes" com.synexia.m3.M3LongIndexPathJNIRecipe --check . "$ROOT/templates"
# To generate in a fresh checkout containing the already merged Java owner:
java -cp "$ROOT/target/classes" com.synexia.m3.M3LongIndexPathJNIRecipe --apply /fresh/checkout "$ROOT/templates"
```

The product receiver mirrors recipe sources byte-for-byte for reproducibility. Subsequent modification of *existing* Java owners must use a source-sealed OpenRewrite recipe; this additive source-admission operation intentionally does not introduce another AST transformation framework. Synexia `develop` is append-only and is never rebased. Donor provenance and category comparisons are tracked in `CATALOG.tsv`.

## Local qualification / acceptance

Executed with OpenJDK `21.0.12.1` and GCC C17 on Linux x86_64:

- Strict `javac --release 21 -proc:none -Xlint:all -Werror` for the original Java owner plus native wrapper and self-test: **PASS**.
- Strict C17 `-Wall -Wextra -Wconversion -Wshadow -Werror -pedantic`: **PASS**.
- Actual loaded library `java -Xcheck:jni`: **PASS, 9,360 differential assertions, 1,600 seeded randomized trials**, including multiple state planes, accepted/rejected masks, negative/non-unit strides, duplicates, signed extreme values, two-pass buffered JNI windows, source/output alias rejection and canary/exception behavior.
- Native-unavailable explicit refusal: **PASS**; no fallback.
- The same full native differential run built with `-fsanitize=undefined -fno-sanitize-recover=undefined`: **PASS**.
- Canonical generator Java strict compile, fresh source emission, exact postimage check and repeat fixed-point application: **PASS**.

The reviewed GitHub Actions workflow performs the recipe Maven build and these selected Java/C17/JNI checks at the PR head. Full target OpenJDK image, large-source memory/swap performance, all collections, Maven/Gradle/Ant forks and cross-platform JNI are separate gates. `JNIEXPORT`/`JNICALL` macros and a `jlong[]` Region path are used for portability, but only Linux has a current execution receipt. Do not infer a runtime speedup from the parity test.

## Algorithm decisions and provenance

The existing M3 int-row native Region selector (M3JDK21 blob `086b49293257fd17cc7fbd1ebbb98460aa677e35`, Apache-2.0) supplies the native-call pattern. The existing Java `M3LongIndexPathMasks` is the sole contract oracle. No LeetCode, HackerRank, GeeksforGeeks submission, proprietary code, or third-party collection implementation is copied. The new first-party Java/C sources and recipe are Apache-2.0; OpenJDK files and all other collection names remain untouched.
