# M3JDK21 indexed live list projection — receiving qualification

**M3JDK21 is one integrated Java 21 / HotSpot / JNI SDK**, not an external "M3 + JDK" layer. This bounded pass modifies the original M3JDK21 `com.m3.collections` owners. Payload references stay in `M3ArrayList`'s dynamic `Object[]` while the view retains primitive physical offsets only. It reuses `M3IndexMaskCompactor` for Java/optional JNI bit-mask selection and extracts the existing stable primitive-index sort of `M3IndexedListSnapshot` to a package-private replaceable behavioral atom `M3IndexSort`. No new JNI entry points or native library are introduced.

The existing `M3FilteredLists` retains the mutable, live-filtered List contract. The **new `M3IndexedLiveView` is strictly read-only** and parent-backed; it caches indexed filtering/sorting but never retains copies of payload references. Structural parent changes and `set` invalidate cached indexes through owner epochs; if callers mutate an object already stored by reference they must explicitly invoke `invalidate()`. Supported operations do not promise thread safety or atomic snapshots. Iterators fail fast after detected parent edits/invalidation.

## Reusable catalogue and mechanical recipe

The searchable TSV is `m3/collections/algorithm-catalog/indexed-live-views-20261021.tsv` and records each algorithm problem category, candidate source, complexity and decision rationale. LeetCode, GeeksForGeeks, HackerRank, Guava and Swift Collections are used only as comparisons or contract taxonomy; **no third-party source is copied**.

Six exact postimage templates and SHA-256 source seals live in `m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-indexed-live-views-20261021/`. The original two source preimages are retained in `pre/`. The existing M3JDK21 receiver implementation of `com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` is reused unmodified; `META-INF/rewrite/m3-indexed-live-views-20261021.yml` names the candidate. The product files were received serially from the templates, refusing Git source-blob drift. Future corrections should regenerate sealed templates and rerun the canonical Synexia OpenRewrite recipe, never manually change generated product outputs.

## Commands and proof limitations

- `python3 m3/collections/qualification/indexed-live-views-20261021/verify_recipe.py` checks each original/source SHA-256 and generated receiver/template exact identity.
- `bash m3/collections/qualification/indexed-live-views-20261021/verify.sh` runs the pin check, strict actual owner Java 21 compilation and normal plus interpreted standalone differential assertions.
- `mvn -B -ntp -f m3/collections/qualification/indexed-live-views-20261021/pom.xml test` runs the real OpenRewrite Java LST receipt test (fresh outputs, fixed point, refusal).

The standalone oracle checks nulls, duplicate occurrences, stable ordering and physical indexes, read-only/no-op mutation refusal, owner structural and set invalidation, JNI-required absence, predicate reentrancy, and a 1,000-step seeded mutation differential.

**Evidence at authoring:** strict Java 21 prototype compilation and 11,707 oracle assertions passed against compatible *minimal test stubs*. These do **not** establish full repository, real JNI, or installed HotSpot qualification. The actual module/JNI/HotSpot/jtreg/JCK, Windows/Android, latency, retained memory and full Maven/OpenRewrite replay are independent gates, and have **not** been claimed as passing. This pass neither implements all 70 proposed annotations nor establishes their javac proof semantics, cross-repo donor convergence or whole-JDK collection replacement.
