# SI warm-cache qualification

The source owner remains `m3/core/src/jdk/m3/text/SharedLexiconCatalog.java`. The SI projection reuses the existing generic payload lookup's warm-first pattern and its parsed payload cache.

`src/test/java/jdk/m3/text/SharedLexiconCatalogSiWarmTest.java` is a standalone post-admission CPU proof. It requires an existing immutable, admitted M3LEX v2 image containing at least two records. The proof opens that image read-only and composes the private catalog state by reflection. It performs no fixture writes. Run modes are `semantics <image>`, `warm <image>`, and `measure <image> <label>`; module-path use requires opening `jdk.m3.text/jdk.m3.text` to the test.

This test does not replace the core contract matrix, manifest/sidecar admission tests, native proofs, or release validation. Its fixture deliberately injects malformed payload data and missing-image state to compare failure behavior. Reflection and counters are test mechanisms; production retains the same cache/storage owner.

The canonical Maven/OpenRewrite recipe, JUnit entry points, sealed RAM compiler inputs, refusal witnesses and exact emitted hashes live in the corresponding Synexia recipe PR. The current81 manifest changes only the source owner row; all original 18 programs and four modes remain required.
