# Current c0 source recipe qualification

This isolated Maven project uses the unchanged `M3Jdk21HashPinnedTextSnapshotRecipe` owner (SHA-256 `8015207ce502d22955f7f77697177e5939fa1e25be7cd74be89c639335ae48e6`). Its authority pins are source commit `d1ecbd43dbadaf218deec0d98a2ddc23f3a3f45c` and M3 commit `c0a14387009aefc7d62bd3268055d526e04f9074`. It contains 143 explicit targets: 69 replacements and 74 additions, grouped into 36 disjoint families of at most seven targets.

The scheduler fixture runs 1,132 complete before/after mixtures within individual families, 611 declared initial refusals, 286 post-scan refusals, and 28 positive empty/unrelated-input cases for the fourteen pure-addition families. Forty-two JUnit methods include the family cases and six shared controls. The two named compositions are the exact Forward and Reverse family orders in `composition-orders.json`; their actual intermediate identities, patches and fixed-point replays are retained. This finite scope does not enumerate a Cartesian product across families, unlisted orders, all possible adversarial inputs or cross-family atomicity.

The actual runtime exports 143 source bodies, an OUTPUT manifest, the concatenated Forward candidate patch, and 144 individual step/replay patches: 289 files. Output qualification flags for receiving consumers, canonical production application and strict canonical admission remain false. These source transformations require separate full receiving compilation, tests, coverage and repository controls before any receiving success can be claimed.

The 120 recovered source candidates are checked against authenticated surviving source identities. The other 23 targets come from the current admitted repair handoffs. The new trial establishes its own provenance. Missing previous producer RESULT, OUTPUT, log and archive bodies remain unavailable; this project does not recreate them, transfer their qualification or represent known historical hashes as present artifacts.

The closed project has 292 logical files. Two before-image fixtures contain significant trailing TAB bytes and must remain exact:

- `src/test/resources/before/m3/tooling/a3/src/test/java/com/m3/a3/A3PlanTest.java`
- `src/test/resources/before/m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/a3-algorithm-catalogue/after/A3PlanTest.java`

A flat published copy may omit these two fixtures. It is incomplete until reconstructed from the associated verified evidence archive; running Maven directly on that incomplete flat copy is unsupported. The companion publication-only `RECONSTRUCT.md` and `PROJECT-READY.json` bind the exact archive, all logical project identities and withheld fixture hashes. The archive's complete project is at logical path `ic0/project`. Using the supplied unchanged archive owner, reconstruct into a fresh destination before Maven execution:

```sh
python3 /path/to/portable/package_evidence.py verify --bundle /path/to/portable/bundle
python3 /path/to/portable/package_evidence.py extract --bundle /path/to/portable/bundle --output /path/to/fresh-extraction
/path/to/apache-maven-3.9.9/bin/mvn -o -B -ntp -Dmaven.repo.local=/path/to/admitted-m2 -f /path/to/fresh-extraction/ic0/project/pom.xml test
```

Use the exact admitted Java 21+35 runtime, Maven 3.9.9 and pinned dependency cache for comparable execution. The recorded qualification uses the frozen `proof.py` preparation/run commands, four ordered stages and sealed inputs; the example Maven invocation is a separate future run and does not update or reproduce an original receipt automatically. Original receipt paths are retained verbatim. Tool, cache and host bodies are not promised as a hermetic portable image.

Three before-image resources also contain authentic NUL bytes. The Python policy before-image is an explicit negative control: its exact path, size and SHA must produce the expected literal-NUL SyntaxError. The repaired policy is parsed normally, as are all other Python sources. Java resource preimages remain byte-preserved input data; no successful compilation of broken preimages is claimed.

No execution result is embedded in this authored README. The external sealed result and its raw receipts determine whether the fresh producer trial passed or stopped. Historical failed trials, unavailable evidence declarations and source authoring revisions remain separate provenance.
