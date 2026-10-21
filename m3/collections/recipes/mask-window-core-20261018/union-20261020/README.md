# M3JDK21 sorted traversal and native-span composition

M3JDK21 owns the implementation. This versioned continuation of the existing mask-window Maven recipe composes selected atoms from product PRs #1549, #1538 and #1547. It does not create a runtime Synexia dependency or another collection hierarchy.

## Exact composition

Receiving parent: `0122f7cf73b3b2a10429806c3e4beadc4caf0792` (#1549, including Windows evidence). Canonical predecessor: Synexia #12549 at `f188e2af9117f0a0126b70aec3f87bb4489cbf42`.

- Preserve #1549's shared Java int/long emission and native independent-offset int kernel, required/excluded masks and nine JNI exports.
- Adapt #1538's ordered byte-offset spans from `a02f0d912ff9da60a5dbc0a7832d7f4b56260fa0`. Read only the enclosing selected span in each nonempty native chunk. This adds 256 stack bytes, not retained row objects. Interior rejected rows may still lie inside the enclosing span.
- Reuse #1547's complete common header exactly, blob `a1bd5aab67eaed2db6fdc3851d85312f24bfc63d`, from `789eafc24ae481724d6c4a2a9a9b75ffd191a6f6`. Use its checked windows and unsigned-byte mask atom. Other #1547 array/residency changes are not received here.
- Reuse actual `java.util.SortedSet` donor blob `ad3ee89d40de8c0ca344863bb6fc4469cec97a09` from #1538. Recursive split children preserve the owner's comparator lazily. This corrects the reproduced comparator defect without a public-interface change; one boundary spliterator object is added per split, not per stored element.
- Keep the current M3FilteredLists (`a9b2f396ae9e952a5cf54cd99e5689b6a23b9517`) and M3FilteredViews (`4a09a43ee8e963155144f88fa0a44255c373088f`) sources in the parent tree. Their full live-wrapper suite is NOT_RUN here. The primitive projection/address owners are exact KEEP inputs and their sort/slice/filter compositions are exercised.

The original M3GpuToolJoinManifest and SealReplay are reused byte-for-byte. No new transformation engine. The nine-row manifest is ordered with the header prerequisite first. Complete preflight precedes serial source writes, with intermediate native and java.base compilation and final whole-recipe fixed points. Five outputs are emitted, including two original synthetic tests; four existing Java/proof inputs are kept. This is finite source-version migration; an older recipe is not automatically a fixed point on successor source. Bind dependent proofs/caches to these new seals.

## Executed qualification

`python3 verify.py --out /absolute/new-exclusive-proof-directory`

The checked driver ran 118 commands from fresh frozen preimages. It compiles and runs actual selected Java/C sources; no replacement runtime stubs. SortedSet is compiled into java.base and executed using stock JDK21 `--patch-module`, not a claimed source-built M3JDK21 image.

- Strict Java21 and C17 warnings-as-errors; fatal UBSan; actual JVM -Xcheck:jni.
- 236,639 Java and 320,006 checked-JNI assertions per run; unchanged prior result digests. Both cross-version Java/C combinations and interpreted candidate pass.
- Baseline SortedSet fails the frozen comparator counterexample; candidate passes 361,273 split-order, reference identity, lazy callback, parallel traversal, reversed/bounded/live, failure and metadata checks in normal and interpreted modes.
- Actual native source under separate function-table instrumentation passes 9,712 span cases / 67,784 checks, including independent offsets, chunk boundaries, both count-drift directions and 32 injected state-read failures. This instrument is not substituted for real-JVM qualification.
- Common header passes 16,777,216 byte-state/required/excluded combinations against an independent per-bit oracle plus 514 boundary/invalid-mask checks.
- Five public descriptor comparisons, generated selector header and all nine JNI exports match. 21 missing-library checks pass.
- All 32 partial output states converge; subsequent replay leaves bytes and timestamps unchanged. Seven malformed-source/template/path cases refuse before writes; two manifest-kernel cancellation cases pass. These are not native cancellation or hostile-plugin sandbox guarantees.

## Performance and rejected work

A singleton selected in a chunk transfers 1 instead of 256 payload elements; the sparse 1,024-row fixture transfers 683 instead of 1,024. State scans are unchanged. Entirely rejected chunks still transfer no payload.

The first ordinal-span composition regressed dense CPU in the local fixture. Its source/timings are retained in the companion bundle. The recipe was corrected to skip identity remapping when a chunk is fully selected, then regenerated from pristine inputs; no output hand-patching. A test indentation warning also stopped an earlier strict compile; the test template was fixed and replayed without relaxing warnings.

Final prepared 262,144-row probe, two fresh forks per variant, 32 warmups and ten measured samples of 20 calls: baseline/candidate median CPU ratios 1.223 singleton, 1.154 sparse, 1.333 all-selected, 0.856 half-selected and 0.991 zero-mask. Half-selected is slower in this run. These are non-JMH observations with host/GC effects; representative performance remains HOLD and no universal speedup or RSS reduction is claimed. The extra ordinal scratch is 256 bytes per active native selector call.

## Rights, boundaries and pending gates

The existing catalogue schema records eleven explicit choices. LeetCode/HackerRank/GeeksforGeeks category evidence is carried forward from the earlier reviewed catalogue, not a new page audit, compatibility proof or source-copy licence. First-party code retains Apache-2.0 notices. SortedSet retains its OpenJDK GPLv2 plus Classpath Exception header. No marketplace/client-private fixtures.

Maven invocation returned 127 (`mvn` absent); network retrieval also lacks DNS. The POM is an entry point to the existing Java recipe driver, not evidence that Maven or the attributed OpenRewrite SDK ran. Full owning-module/JUnit, installed M3JDK21/jtreg/JCK, full live-wrapper qualification, cross-platform union and representative performance remain NOT_QUALIFIED. Earlier Linux/Windows/parent receipts are historical, not this union's platform proof. Full chat-program completion and all donor branches are not claimed.

Keep draft / HOLD_FULL_GATES. No merge, rebase, force-push, default-branch edit, permission change, deployment or automatic promotion.
