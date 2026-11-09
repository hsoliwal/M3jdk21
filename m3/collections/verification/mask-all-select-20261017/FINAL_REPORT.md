# M3JDK21 exact-mask receiver — selected-source delivery

**Draft, review required.** The canonical first-party source adaptation and Maven/OpenRewrite recipe remain owned in Synexia PR #12052. Target M3JDK21 receives one existing Java implementation atom in the already independently built Apache-2.0 collections module, plus target-only provenance, tests and workflow. The public Java collection names, native method symbols and existing JNI C file are unchanged.

The optimization changes `countSelected(0)` to return the known logical length without scanning, and for all selected rows returns a defensive primitive-array clone rather than performing another mask test on every row. Sparse selections retain exact-size two-pass copying. No retained entry objects, extra index metadata, JNI copy, or second collection tree is created. Complexity remains O(n) worst-case with O(k) final storage.

Selected-source Java21 and original JNI parity pass; this cannot waive full Maven/OpenRewrite SDK, target Maven/JUnit, full JDK, jtreg, independent review or CPU/heap/platform benchmarks. See `STATUS.tsv`.
