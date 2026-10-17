# Filtered/sorted wrapper contract refinement

This successor is based on M3JDK21 PR1018 at `0997991e2496ee07b69be5d245ebc0ed1bd9b1f3`. It changes two existing Java owners and adds two proof files. It does not add a competing collection hierarchy or filtered List overload, alter java.base, or claim convergence of all independent keyed/list/primitive branches.

## Runtime changes

* Filtered Set and projected map key/entry spliterators preserve `DISTINCT`; mapped values do not. The flag survives splits and avoids unnecessary downstream distinct hashing. Cardinality remains unknown (`SIZED` and `SUBSIZED` are absent).
* Filtered SortedSet/NavigableSet cursors derive `SORTED|ORDERED|DISTINCT` and comparator identity from their authoritative owner, even if a bounded/descending source cursor omits metadata. This does not sort or copy rows.
* Existing Collection and Map size scans share a saturating nonnegative increment. All predicate occurrences are still evaluated; early truncation is not substituted. Actual >2^31 repeated-element witness included, without a retained element array.
* `isEmpty`, `containsValue`, and `hashCode` borrow exact first-party raw-entry scan bodies from PR994. Public entries still use guards. No new runtime dependency, payload/index owner, native handle or public API descriptor.

Live backing semantics, admitted equals/comparator-key owner profiles, hidden mappings, duplicate values, null policy, encounter/predicate order, immutable retained predicate prefixes, range/reversed views and snapshots remain. Explicit predicates must satisfy the existing purity/stability contract. External synchronization remains required. Sorted comparators must satisfy the admitted Set/Map equality contract; no repair of inconsistent client comparators is attempted. Per-cursor flags/comparator references add metadata; no universal retained-memory or throughput win is claimed.

## Recipe custody

`com.synexia.m3jdk.WrapperContracts20261017` uses the unchanged `M3HashPinnedJavaSnapshotRecipe` in the existing Maven module. Four sealed Java postimages: two exact predecessor postimages as preimages and two ABSENT-before tests. SDK test covers real named execution, Java LST identity, fixed point, all single-target partial deliveries, unrelated input, missing/drift/occupied/non-Java/duplicate refusal, and exports actual Results only after all its assertions.

The incumbent Java/Maven `Replay` is reused with exactly two literal bindings recorded in `REPLAY_BINDINGS.json`; its validation/staging/locking code is unchanged. The verifier qualifies it and compiles/tests the files it actually emits. No manual postpatch. This direct Java execution is **not execution of Maven or the OpenRewrite SDK**. It assumes an exclusively owned cooperating worktree, provides atomic per-file publication, not a repository transaction. Mixed PRE/POST is refused by offline replay (SDK separately tests partial delivery).

## Reproduce

```sh
python3 m3/collections/wrappers/contract-refinement-20261017/verify.py \
  --root . --output /tmp/m3-wrapper-contract-proof-fresh
mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml test \
  -Dtest=M3DeepFilterRecipeTest,M3WrapperContractsRecipeTest \
  -Dm3.deep.sdk.output=/tmp/m3-deep-sdk-fresh \
  -Dm3.wrapper.sdk.output=/tmp/m3-wrapper-sdk-fresh
```

Output directories must be absent. Verification requires Linux, JDK21, GCC, and Python3; Maven plus pinned dependencies for the SDK stage. The existing exact-head workflow retains the predecessor SDK/Java/JNI checks, qualifies the successor, verifies predecessor-post = successor-pre and successor-SDK = successor-replay = installed output. Four unchanged predecessor proof targets retain their original installed seal.

Local evidence: 32 completed commands; 13 refusal cases leave source bytes unchanged; four actual emissions and no-write replay/check fixed points; 964,437 checks under both normal and interpreter modes (192 seeds x five owner profiles); inherited deep composition 1,312,160 checks at depth65,536 and 165,280 at depth8,192 under interpreter; actual 2,147,483,664-occurrence size witness; both public facades `javap -public -s` identical; 156 JNI checks under `-Xcheck:jni` and fatal UBSan. These check counts include repeated/structural assertions, not independent test methods. The unchanged JNI kernel and forwarding proof exercise primitive projections, not native predicates/sorting or a new production acceleration.

`LOCAL_RECEIPT.json` and `COMMANDS.log` retain completed local evidence. `EXPERIMENTS.md` discloses earlier failed candidate/aggregate attempts. Maven/OpenRewrite SDK, full owning module/reactor, other native platforms, source-built JDK/jtreg/JCK, memory/throughput performance and complete collection-family convergence remain independent gates. Keep the PR draft until required exact-head evidence is green.

## Search and reuse

`CATALOGUE.tsv` is category-searchable, e.g. `grep -E 'set-projection|cardinality' CATALOGUE.tsv`. `DONORS.tsv` pins implementation/proof providers; `FILE_PASSES.tsv` records per-file pre/post identity. Challenge sites are taxonomy evidence only, separate from licensed implementation donors. No third-party solution code or JDK implementation was copied/relicensed. All first-party Apache-2.0 notices are retained.
