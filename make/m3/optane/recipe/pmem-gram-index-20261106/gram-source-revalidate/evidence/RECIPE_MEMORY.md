# M3 recipe correction memory

- Stage/kind/signature: `SOURCE_GENERATION / PRUNED_GRAM_FALSE_NEGATIVE / complete=true_after_same_length_external_write`.
- Original #16571: valid donor/quality review completes, but `source.bin` can change externally before/during gram search; a stale index prunes a newly matching file, yet reports complete.
- RED: reviewer changes `D.java` from `zzz` to `abc`, source byte length unchanged; parent code returns two matches and accepts incomplete work.
- GREEN: after original exact search, check every selected original source with `PmemSourceImage.readSealed`, throw `SecurityException` on D before creating candidate tasks. Separate injected mutation during the search must also refuse.
- Admission constraints: unchanged native/JNI symbols, original Maven recipe owner, contract and source roots, UTF-8/binary semantics, cancellation and no duplicate model call.
- Local executed JDK21: 20k seeded source records, 274 matching tasks, baseline 846 checks unchanged, original/new `javap` descriptors identical; a FILE test double executed six Jupiter-shaped cases. Actual JNI and Maven SDK NOT_RUN.
- Improvement for a future separately qualified pass: reuse the original PMEMSDK caller-owned source read buffer (sibling source-slices PR), but do not borrow source that has not landed in this exact receiving branch.
