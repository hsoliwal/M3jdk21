<!-- SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 -->
# M3JDK21 composite String — existing rope plus lean cursor selection

M3JDK21 is the product, not an M3-on-JDK wrapper. The source of truth stays
`java.lang.M3String` (only an owner reference and packed coordinate),
`M3StringTuple` (canonical balanced DAG), `M3StringPool` (native/mapped
immutable scalar pieces and bounded tuple interning), and the existing
`M3String.CodeUnitCursor` (thread-confined, scalar-run traversal).

This change does not create a list, tree, trie, copy of characters, side cache,
JNI method or another String hierarchy. It **selects an existing execution
strategy**: long prefix scans across tuples use the already-qualified leaf
cursor, rather than asking each character to descend the rope again.

## Private atoms and exact semantics

1. `M3String.startsWith(M3String,int)`: retain original bounds, same-coordinate
   fast path and prepared conservative prefix/suffix facts. For length >= 64
   and at least one composite owner, create at most two request-local
   `CodeUnitCursor`s, compare exact UTF-16 code units, and return immediately
   on mismatch. For short/flat cases use byte-for-byte unchanged direct
   comparison.
2. `M3StringPrecomputeBridge.startsWith(M3String,String,int)`: retain original
   flat-prefix admission (never intern merely for a comparison), prepared
   negative-gate behavior, and Java String callback semantics. If the source
   is a tuple and the flat prefix has >= 64 UTF-16 units, reuse one source
   leaf cursor. Other cases follow the original comparison.
3. `M3StringCompositePrefixCursorTest`: public String and original private
   owner/bridge comparisons against a separate char[] oracle. Test compact
   and noncompact modes, 63/64/65 threshold, empty and invalid offsets,
   257-piece DAGs, independent composite prefixes, Unicode/surrogate seams,
   random slices, hash/UTF-8 output and canonical owner/coordinate stability.

The source unit may be in a tuple branch, slice, immediate or mapped atom.
The cursor already respects the child source range and retains no native
address across callbacks. This optimization creates no persistent state
and does not change JNI exports, String field shape, API or GC-visible
ownership. Existing JDK String behavior remains authoritative.

## Complexity and memory

For a prefix of m UTF-16 units over a p-piece balanced DAG, repeated
recursive `charAt` can take O(m log p); the cursor resolves a scalar run
once and scans consecutive units, typically O(m + visited leaves x log p)
with two constant-size transient cursor objects. Early failures and short
scalars keep the original no-allocation path. No extra resident directory
is admitted or materialized.

A separate Java21 **stand-in** pair of equal, independently owned 8,192-leaf
balanced ropes (90,340 code units) showed six warmed cursor/direct ratios
0.204, 0.220, 0.207, 0.204, 0.209 and 0.210. A second 118,758-case
randomized stand-in comparison over composite ranges passed. These are
algorithm-direction evidence only, **NOT a claimed M3JDK21 speedup**.
Product throughput, allocation and retained heap must be measured on
the same complete M3JDK21 build before any performance claim.

## Recipe and evidence

The exact preimages and proposed postimages are in
`m3/tooling/shared-frontend/recipe-custody/datasets/m3-prefix-cursor-20261020`.
The named Java LST recipe reuses the product's existing
`M3HashPinnedJavaSnapshotRecipe`, with exact source SHA-256, fixed-point,
drift refusal, test sources and generated file integrity gates. No fresh
transformation engine and no donor body is copied. The product README and
catalogue supplement its canonical source custody without shadowing it.

Run:

```sh
root="$PWD/m3/tooling/shared-frontend/recipe-custody"
mvn -B -ntp -f "$root/pom.xml" -Pcontract \
  -Dm3.dataset=m3-prefix-cursor-20261020 \
  "-Drecipe.owner.root=$root/sdk-owner" verify
python3 "$root/receive.py" --dataset m3-prefix-cursor-20261020 \
  --recipe-owner "$root/sdk-owner" --target "$PWD" --check
```

Compile the new jtreg source with a stock Java21 javac to check test syntax.
Execute the test and existing M3String canonical DAG, join carry and UTF-16
differential/JNI gates on an **actual source-built M3JDK21 image**. A queued
or runnerless workflow is not a passing runtime gate.

## Conflict gate

Open M3String source-owner PRs, including #1530, #1531 and #1456, also
touch `M3String.java`. Do not overwrite them: this proposal is source-sealed
to current master and requires a forward recipe if the owner changes first.
The existing M3StringPool, per-owner facts, lexicon indexes and JNI code
remain unmodified.

Defer additional permanent list/B-tree/Fenwick/trie indexes until
operation-specific repeated-use profiles prove a net advantage after memory,
construction time, GC and invalidation overhead. A mutable piece table
belongs to a document/edit session owner, not automatically to every
immutable M3 String.
