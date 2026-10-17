<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Java-util filtered/sorted view contracts

Canonical owner: Synexia, child of `synexia-m3-recipe/recipes/m3-java-util-views-20261017`
at parent `4541f77950157f9d75d95b24a5caf785cf4a0341` (PR12015).
Product baseline: M3JDK21 PR1038, `67feb628e55d886d4a82fd8bc4e8a3a63fe2c967`.
This is a selected correction, not completion of every collection.

## Existing owners and observable boundaries

`java.util.CollectionViews` remains the sole implementation behind the unchanged
seven-factory `com.m3.collections.M3CollectionViews` facade. Live filters, source-owned
ranges, stack-safe predicate plans, visible-only mutation and detached stable snapshots
remain. No new public signature, payload owner, interner, sort engine or runtime dependency.

The same-author typed-metadata atom from sibling PR1036 is adapted without importing
its other predicate-chain/map implementation. Filtered Set cursors preserve DISTINCT;
SortedSet cursors preserve DISTINCT/SORTED/ORDERED and owner comparator identity through
all splits, including bounded/descending sources. Lists keep duplicates; filtered cursors
never claim SIZED/SUBSIZED/CONCURRENT. Two private per-cursor fields are added, not per-entry
state. Comparator retrieval happens at cursor construction; stable/pure owner semantics
remain required. No equivalence for arbitrary stateful comparators or nonconforming identity
Sets is claimed. The previously faulty missing metadata intentionally changes.

Size saturates at Integer.MAX_VALUE while all predicate occurrences are evaluated.
The actual old implementation returned -2147483632 for 2147483664 logical occurrences;
the candidate returns 2147483647. This virtual fixture does not allocate billions of objects
or establish physical CPU callback counts after JIT optimization.

The existing `RunSearchJni_first` test kernel now validates 512 runs per region transfer
(maximum1024ints,4096-byte buffer payload), without native heap allocation or whole-array
pinning. It still validates every run after locating the answer, preserves cross-region
ordering and pending exceptions, and ignores unused capacity. Stable input is required;
resource/OOME behavior can differ. It is verification-only JNI, NOT production acceleration,
libjava integration, a transaction, zero-copy, or a native throughput improvement.

## Recipe and executed proof

Named `com.synexia.m3.JavaUtilViewContracts` reuses byte-exact M3GpuToolJoinRecipe and
M3GpuToolJoinManifest. Task-local source mirrors retain canonical provenance in INPUTS.json.
The five-row manifest seals three changing files and two KEEP owners. This is the existing
exact-source PlainText SDK lane, not attributed Java rewriting. `author.py` writes candidates
only; actual OpenRewrite Results supply serial Java -> C -> test receiving bytes. Fix the
recipe/templates and regenerate, never patch receiving output.

Two complete fresh offline Maven verify runs passed, the second from a packaged task copy:
five exact JUnit methods, zero failures/errors/skips, three SDK Results and no-change fixed
point; eight partial states across three orders; worker/refusal/tamper/cancellation checks.
Maven verify requires the runtime verifier after tests. It ran58commands with expected outcomes:
28559 legacy checks with unchanged tape;705766 combined checks in normal/interpreter/C1/C2;
2147483664-occurrence size witness;2606 real-JNI checks under checked JNI/fatal UBSan and release;
162 mock scenarios/3316checks supplement real JNI. Original defects and three compiling mutants
fail their intended witnesses. Public facade descriptors, JNI header/export, binary-client
linkage and repeated class bytes are checked. Counts overlap and are not exhaustive proof.

Selected real sources were patched into stock Java21 java.base, NOT a rebuilt M3JDK21 image.
SUMMARY.json contains outcomes. RAW_RECEIPTS.json binds original failed/completed Maven logs,
raw command/stdout/stderr and Surefire XML in the conversation review archive; those raw bytes
are not in this Git tree. The initial stale mutant anchor failure was corrected in verify.py,
not in runtime templates. No binaries, toolchain, credentials or fonts are shipped.

## Reproduce and review

With the approved offline Maven/cache and a fresh absolute proof directory:

```sh
python3 author.py
mvn -o -B -ntp -Dmaven.repo.local=/approved/repository -Dproof.root=/absolute/new-proof -f pom.xml verify
python3 check-receiver.py --root /path/to/M3jdk21 --sdk /absolute/new-proof/sdk-results
```

The final command checks installed sources and never writes them. INPUTS.json pins toolchain,
source and engine identities. FILE_PASSES.tsv binds per-file atoms to digests. CATEGORY_REVIEW.tsv
adds13searchable comparisons; the parent12-row catalogue stays unchanged. LeetCode/HackerRank/GFG
are category references, not copied solutions. No universal speedup is claimed.

Keep draft for source-built JDK, jtreg/JCK, full reactor/provider/identity/concurrency/serialization,
platform, hosted CI, resource/performance and global naming/catalogue admission. Existing java.base
Apache-source explicit-relicense/target-owned admission remains HOLD; location does not relicense
source. The local mapping records this correction without advancing String -> arrays -> collections.
