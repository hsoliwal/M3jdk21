# Provenance and licenses

The production M3String snapshot retains its GPLv2/Classpath-exception header unchanged. OpenJDK LICENSE and ADDITIONAL_LICENSE_INFO are retained in the SDK closure; no Apache relicensing applies to target snapshots. The three canonical Synexia recipe providers retain their Apache-2.0 headers. Maven/test/checker adapters derive from the exact pins in FOSS_REUSE_DECISION.tsv; the actual target test reuses the existing Apache-2.0 differential corpus and the public CharsetEncoder replacement API pattern documented by the OpenJDK String coding test. No donor runtime, allocator or native pointer implementation is replaced.

Existing CodeUnitCursor calls scalar.charAt, preserving atom reachability fences. The authoring adapter only binds current snapshots to the existing Maven/OpenRewrite engine; a source-only qualification is not target Java/JNI runtime or performance proof.
