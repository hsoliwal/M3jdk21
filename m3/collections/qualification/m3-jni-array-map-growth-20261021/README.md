# JNI array storage receiving candidate

Canonical recipes and exact execution inputs: [Synexia commit b096b2b3c1904bb0f32e1cc8802e5bec7fd64ced](https://github.com/hsoliwal/com.synexia/commit/b096b2b3c1904bb0f32e1cc8802e5bec7fd64ced), [draft PR #12835](https://github.com/hsoliwal/com.synexia/pull/12835).

The five source changes reuse the supplied JNI allocation owner and retain array payloads, primitive slot/index/order/free lanes and flags. Current shared JNI-core changes and the Maven boot profile survive. Production Slot initialization remains unchanged.

Actual proof: Map SDK5 and six fresh JVM modes/10,077,334 checks; current C SDK5, C17 warnings-as-errors compilation and 18,612 JNI/type/identity/negative/overflow/GC/UTF16 checks using its newly built DLL; current POM SDK7. The six Map modes used the previous qualified allocator DLL. All historical failures, source seals and source/advisory records remain in the canonical crate.

This remains a draft. Full target reactor/jtreg/installed-image, C++20, combined current-DLL Map replay, concurrency/serialization, Unicode-path loading and LFU API projections remain pending. The native-empty Slot experiment remains rejected because of its four reentry counterexamples. Formal autoreview is NO_VERDICT: its configured Codex helper rejected the required --no-tools option before an engine ran. Neither copied source nor finite tests establish the whole superset or peak performance.

HANDOFF.json binds exact source hashes, Git blobs and canonical custody. Proof JSON preserves the actual execution paths as evidence. No dependency binaries, DLLs, classfiles, caches or unrelated changes are included. Storage protection remains active.
