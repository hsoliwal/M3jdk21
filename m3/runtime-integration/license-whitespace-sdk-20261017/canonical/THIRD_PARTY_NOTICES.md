# Third-party notices

The standalone qualifier reuses the Apache-2.0 Synexia manifest kernel and OpenRewrite adapter byte-for-byte at Git blobs `38df5f97bbeec8a8a8cfc77091d56bda925c07f2` and `1c1b1b568d369f688f6953161636bf36b484bf5a`. Existing SPDX headers are retained. The exact receiving-input and Surefire report verifier is adapted from the existing Apache-2.0 source-owned SDK qualifier. Its FOSS decision is retained in `FOSS_REUSE_DECISION.tsv`.

The opaque OpenJDK donor notices are retained verbatim in `licenses/ADDITIONAL_LICENSE_INFO`, `licenses/ASSEMBLY_EXCEPTION`, and `licenses/GPL-2.0.txt`; their original terms and copyright notices apply. The duplicate policy before fixture is the identical ADDITIONAL_LICENSE_INFO Git blob. No license text is normalized or trimmed.

The unchanged standalone POM pins OpenRewrite core 8.17.1 (Apache-2.0), JetBrains annotations 24.1.0 (Apache-2.0; provided), JUnit Jupiter 5.10.2 (EPL-2.0), and slf4j-simple 1.7.36 (MIT; test). Maven plugins are used at their already pinned versions.

Git is an external GPL-2.0 executable used only by the bounded isolated test probe; no Git implementation source is copied. The qualifier adds no complete JDK build, image, resource, or jtreg output admission.
