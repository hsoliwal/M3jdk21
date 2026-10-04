# Type proof execution record

One existing Java owner is repaired by reusing the already-present typed SourceFile assignment pattern. Current PR89 preimage d2b546e03bc181a244978c1c3d58bc4a93faaad4 is replaced by fc69c5bcde6ef33ae925a67c1969971a390dcc53. The original preimage resource in PR89 remains immutable; its reviewed output/hash are corrected. Existing tests remain unchanged.

The new MetaTest contains four actual OpenRewrite/JUnit tests: metadata/checksum retention, missing/drift/duplicate refusal, changed-after-scan refusal, and exact workflow/document replay with fixed point. They are authored, not executed locally. The existing workflow still runs the complete module clean verify and unchanged JaCoCo policy, requires non-skipped metadata/parent tests, and saves real reports on failure. Its source archive is labelled INPUT_NOT_PROOF.

Executed: exact source/diff review, two-source JDK syntax parse, YAML parsing, ordered manifest/postimage hashing, 14-file whitespace checks. Maven is absent (exit 127); no SDK stubs or invented framework results. The full configured dependency compile, JUnit scheduler, JaCoCo, modified JDK, native and jtreg gates remain open.

Public API, emitted-source contract, metadata update order, charset/BOM, checksum invalidation and identity operations are unchanged. Tests intentionally transform a tiny value fixture only inside their isolated source set. No new JDK runtime API or native code is introduced. Feature history is additive and canonical branches are not written.
