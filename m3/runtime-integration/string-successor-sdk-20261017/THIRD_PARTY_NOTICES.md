# Reuse and rights

- Synexia's existing M3GpuToolJoinRecipe (Git blob 1c1b1b568d369f688f6953161636bf36b484bf5a), M3GpuToolJoinManifest (38df5f97bbeec8a8a8cfc77091d56bda925c07f2), historical recipe and SDK tests are reused under Apache-2.0 with notices preserved. The generic adapter name does not include or authorize unrelated GPU templates or private product code.
- OpenRewrite rewrite-core 8.17.1, Apache-2.0, is used as an unmodified Maven dependency. Official tag APIs establish interface fit; actual SDK execution remains required.
- JUnit Jupiter 5.10.2 (EPL-2.0) and SLF4J simple 1.7.36 (MIT) are unchanged test dependencies from the qualified receiving module.
- M3jdk21 public M3String, M3StringFacts, M3StringPositionPrecompute, M3StringPool and M3StringSearchPrecompute source fixtures at 970a34350ebdf6a3f1c5ff5acc8445d0f3515f55 retain original copyright and GPLv2/ClassPath-exception headers. They are separate opaque test data. GPL, additional licensing and assembly exception texts are copied byte-exact in sdk/licenses.
- The searchable taxonomy remains the existing String/source-custody problem category. No challenge, editorial or submission implementation is copied.
- Preparation-only SHA-256 calculations used unmodified js-sha256 by Chen Yi-Cyuan, MIT, at emn178/js-sha256 commit 7912494a5ae277660131557401b91f11f47c96d2, source blob906663b3e8345e17b1ccfaf6eeb4df4bce893e58. It was evaluated in ephemeral tool RAM, not shipped as a product dependency; known empty/abc/JSON/Unicode vectors were checked. Python hashlib remains the checked-in verifier.
