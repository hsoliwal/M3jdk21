<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Apache multivalue source custody: additive v4 → v5 receiver

This is a **source-custody successor**, not a replacement implementation or a waiver.
Original `SDK_SOURCE_BINDING_SUCCESSOR_V4.json`, `verify_source_binding_successor_v4.py`,
`probe_source_binding_successor_v4.py`, the v3 receipts and the v2→v3 OpenRewrite
recipe remain byte-for-byte unchanged.

On [M3JDK21 Apache multivalue CI #41459436384](https://github.com/hsoliwal/M3jdk21/actions/runs/41459436384),
v4 refused an earlier source pin for `M3ArrayList.java` before it could run
its original candidate tests. Of its ten current-source successor rows, seven
still match byte-for-byte; exactly three changed on the integrated receiving tree:

| Owner | v4 source blob | Current receiving blob | Semantic change |
| --- | --- | --- | --- |
| `M3ArrayList.java` | `dda9d3739f4f7f779bd29e92c80bbc4d45ae1063` | `3c6e1b12a40d5516d8b1ca0c8ed9fc86c99003f1` | Stable, two-pass `removeIf` uses two long mask words for 65–128 slots instead of allocating BitSet |
| `M3LongRangeSet.java` | `a19f3a01b8c553a8c9f356662edfc9fadc8194c8` | `ad7826be14ba09539e55125f52a6bea7a58ee362` | Sorted interval binary bound, cardinality accumulation and allocation-lower Java mask selector |
| `m3_index_mask_compactor.c` | `970a7074223bfdff2eea70ad18fc2062c78b8b5b` | `5afeb41575e758b06882b3e3d8104b0c275f50ca` | Native mask/array compactor changes must retain original strict C17/JNI contract |

## Explicit immutable-source replay

The new `verify.py` pins the **original v4 receipt and verifier**, pins the
three current effective source blobs, and pins the **old exact v4 source bodies**
in `snapshots/`. It reconstructs the old v4 input tree under a throwaway
directory from the repository's still-current unchanged source inputs plus those
three frozen historic images, invokes the **original v4 Python verifier**
unchanged, and refuses any changed/missing source, predecessor, or snapshot.
For contracts requiring the original provider JAR, `--jar` retains v4's exact
external-provider hash/size gate. `--probe` additionally runs the unchanged
original v4 adversarial CLI probe on the historical replay tree, including
negative mutation/rollback tests. The v5 verifier never writes or silently
changes the real `m3/collections` production sources.

This means **original v4 admission must actually pass** before newer
sources are admitted. The host then runs the **existing real** Apache/SDK Java
tests, differential original-provider oracle, standalone Maven/OpenRewrite
tests, and C17 `-Xcheck:jni` contract, without editing their implementation
bodies. A five-method v5 regression suite covers the positive replay and
adversarial current/historical mutation, traversal and output-alias refusals.

The canonical Synexia `M3HashPinnedTextSnapshotRecipe` packet
`doop-key-value-apache-v5-20261102` owns the complete exact text
PRE/POST images, fixed-point and refusal qualification. This receiver is not
an excuse to overwrite the historical v4 hashes with the latest commit hashes.

## Catalogue donor dispositions

- **LeetCode 27 / GFG in-place compaction:** Stable array compaction is a
  technique donor; unchanged public `List.removeIf` semantics and fail-fast
  epoch are authority. Bit flags avoid per-entry objects and the 65–128-slot
  allocation.
- **LeetCode 34 / 57 / GFG lower-bound:** An indexed binary endpoint supports
  sorted interval insert/removal. Current range-set's half-open signed-long
  contract, exact cardinality and native-required refusal remain authority.
- **HackerRank Array Manipulation:** Its offline difference-array algorithm
  is a *rejected functional donor* for dynamic live interval removal, because
  inclusive offline updates are not equivalent to the existing APIs.
- **JNI region APIs:** Use the existing bounded native implementation; no
  `GetPrimitiveArrayCritical` or retained heap borrow is introduced.
- No competitive-programming solution body or third-party implementation is
  copied. Preserve each existing donor/source license and attribution.

**Merge HOLD:** This packet records source lineage, not full compatibility.
Hosted Maven/OpenRewrite SDK refusal/fixed point, actual Java21/C17/JNI
and provider-differential contracts must all pass on the *same* reviewed head;
full JDK image/jtreg/JCK and unrelated collection family gates remain separate.
