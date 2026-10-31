# M3JDK21 filtered primitive sequence receiver

This candidate installs one byte-identical Synexia recipe postimage in the existing separate `m3/collections` Java21 module. `M3LongArrayList`, `M3LongArrayDeque`, `M3LongList`, `M3LongDeque`, `M3LongQueue`, `M3LongStack`, their interface identities, and existing native C/JNI owners are untouched.

`M3PrimitiveFilteredSequencesTest` runs a deterministic JDK-model contract against **actual existing primitive backends**, not the Synexia fixture doubles. No promotion before those JUnit tests, Maven/OpenRewrite SDK, full JDK runtime acceptance, jtreg, JNI/GC validation and independent review have actual green receipts. The stock String/array phase remains a separate gate.

Run `bash m3/collections/verification/primitive-filtered-sequences-20261017/verify.sh` on a checked-out M3JDK21 tree with JDK21. This direct product source-path verifier compiles against the REAL M3 backed implementations (not the synthetic Synexia fixture); its actual exit receipt remains pending.

Canonical source recipe: [Synexia PR #12186](https://github.com/hsoliwal/com.synexia/pull/12186) pinned at `8ede0d597f402d3ba2c77eea8d3f4d2ab8cee76a`. This receiver must retain that exact source identity and its Apache-2.0 attribution.
