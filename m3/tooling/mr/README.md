# MR — Matcher reuse and MIndex handoff

MR is a target-owned migration increment. Synexia supplies algorithms, recipes,
contracts and evidence; M3JDK21 owns adapted runtime code. The public types remain
`String`, `Pattern`, `Matcher` and the existing collection APIs. The internal
String name is **M3 String**, implemented by `java.lang.M3String` and its current
owner/coordinate, atom, tuple and fact owners. No Synexia runtime dependency is
introduced.

## Runtime change

`Matcher` already owns bounded `M3TQ.Facts` for an exact immutable String and
region. Previously `reset()` discarded those facts even when the input and
region were unchanged. MR retains the facts across reset and clears them before
`reset(input)` changes the input reference. The existing exact region check
still decides reuse. A different but equal String intentionally invalidates.
Mutable inputs still bypass preparation. Failed new-input reset releases the old
facts; a rejected region operation leaves the current state intact.

This is derived metadata, not match state or a saved `MatchResult`. All existing
group, append, boundary, hitEnd/requireEnd and mutation bookkeeping remains in the
JDK implementation. Case-sensitive `Pattern.LITERAL` find is the only existing
consumer. A negative trigram result proves absence; a positive result still runs
the exact regex engine. `matches()` and `lookingAt()` retain their existing paths.

No Matcher fields are added. The historical removal of a second retained input
reference survives. A matcher retains at most one existing bounded fact packet
until input replacement, subsequent region preparation, or matcher collection.
The packet owns primitive trigram metadata, never another spelling store. The
existing 32,768 UTF-16-unit region ceiling and optional-precompute OOME fallback
remain unchanged. Reset may now extend the lifetime of that already-bounded
metadata while the same matcher/input remains live; there is no global cache.

## How the whole structure moves

The migration is a many-to-many capability adaptation, not a prefix replacement.
`m3/docs/name-mapping.json` remains the authority. This packet preserves its
existing 52 records and 20 gates unchanged, adds a Matcher lifecycle record and
explicit pending RXM/RXA records. The serial port plan remains
`m3/docs/m3-porting.md`. `evidence/census.tsv` is a pinned review queue, not another
registry or a claim of semantic coverage.

| Donor responsibility | M3JDK21 destination | Admission work |
| --- | --- | --- |
| Canonical MIndexString atoms, ranges and composition | Existing M3String, M3StringOwner, M3StringAtom, M3StringPool, M3StringTuple | Preserve exact UTF-16, object identity distinctions, ownership and seam behavior; never retain a second payload |
| Fixed-size character, hash, whitespace and boundary facts | Existing M3StringFacts | Port missing compatible atoms into this owner; hash/filter hits do not prove equality |
| Literal search plans and position masks | Existing M3StringSearchPrecompute and M3StringPositionPrecompute | Keep bounded metadata, owner/coordinate keys, exact verification and memory-pressure behavior |
| Regex trigram facts and candidate rejection | Existing M3TQ and Pattern/Matcher | MR reuses completed same-input facts; flags, region and provider semantics remain explicit |
| RXM full-input capture result packets | Pattern/Matcher, after state-contract adaptation | PENDING: RXM is not a snapshot of arbitrary Matcher region/bounds/append/reset state |
| RXA prepared automaton closure tables | Regex-layer implementation only after provider proof | HOLD: restricted RE2/J-oriented behavior has retained JDK counterexamples; do not install it behind Pattern by renaming it |
| Primitive collection storage and bit arithmetic | Existing internal M3Address28, M3IntLane28, M3LongLane28, M3BitLane28, M3Bits | Preserve sparse allocation, range checks, alias/overlap, rank/select and native ABI before adding consumers |
| Higher collection owners, maps, sets, rings, sorted indexes and views | Existing M3 collection owners or the matching java.util implementation | Qualify by ordering, equality, mutation, views, serialization and concurrency; no universal M3 container |
| Class metadata and immutable precompute context | Existing M3CI, M3CB, M3PC and Class bridge | Keep defining-loader/context identity and lifecycle invalidation; public Class remains Class |
| Compiler/AST facts and recipe convergence | Appropriate javac/tooling owner and retained A3/convergence recipes | Keep compiler/Maven/OpenRewrite dependencies out of java.base; no second laboratory |
| JNI and mapped/native backing | Existing M3 String backing, libjava and HotSpot owners | Generate target symbols/headers and prove lifetimes, encodings, ABI and image execution; donor JNI proofs are not target proofs |
| GPU/batch/search images and application services | Appropriate optional target module or explicit deferred donor capability | Require a concrete target consumer and bounded residency; never put all donor services into java.lang |

Collection elements must not require a retained Entry/Node wrapper per element.
Public Map.Entry projections remain fresh caller-owned compatibility objects.
Use direct/sorted/ring/segmented/sparse storage where the operation contract fits;
hashing is one lookup choice. Generic keys still obey their original equality,
identity, comparator, null, callback and concurrency contracts. Optional residency
may shrink, but canonical payload and observable collection semantics may not.

The queue covers every `src/main` blob in nine exact Synexia subtrees, including
non-prefix helpers, native sources and resources. It does not cover all repository
modules, generated/test closure, nested symbols or semantic dependencies. Every
queue row is REVIEW_REQUIRED. Existing catalogue records supply known mapping IDs;
unmapped rows remain under `family.entire-source-closure`. No inventory row grants
mutation or runtime acceptance.

Recommended serial port order: close canonical storage and helper dependencies;
reuse structural lanes and views; add operation-specific precompute consumers;
qualify stateful regex and collection behavior; then qualify native/JNI/HotSpot,
GC/JIT/CDS/serviceability and supported platforms. Each step carries exact donor
revision/path/license, target pre/postimages, recipe identity, refusals and target
evidence. Useful Synexia source remains as donor/reference implementation.

## Recipes and proof

The crate runs the retained `M3Jdk21HashPinnedSnapshotRecipe` for Java and
`M3Jdk21HashPinnedTextSnapshotRecipe` for the canonical map, port map and existing
workflow. The `jdk22-` resource prefix is the retained installer's crate namespace;
MR is not a JDK22 backport. Both recipe orders converge to identical postimages
and a no-change second pass. Exact-source drift, missing/duplicate owners, wrong
parser trees, occupied additions and template tampering are refused. Unrelated
sources survive. No new parser, compiler or convergence engine is introduced.

The TQ successor custody check now handles the shared naming map in the same
way as the retained TQ gate: prior records, their order, every other authority
field and all gates must remain exact; later records may be appended with unique
IDs. All other target postimages and dependency guards remain byte-exact. Tests
reject record removal, changes, reordering, duplicate IDs and changed gates or
authority. This does not validate or promote a new port's runtime behavior.

The current String invariant script also contained five unterminated multi-line
`if ... or ...` conditions and could not parse. The text recipe repairs their
parentheses and reconciles stale route spellings with the current same-coordinate,
prepared-hash and exact-comparison owners. Exact comparison markers remain required.
The malformed workflow-path check is scoped to path-filter entries, so legitimate
space-separated jtreg arguments are allowed. Five fixture tests execute the gate
and reject lost UTF-8 checks, removed exact equality/comparison and joined filters.
The full invariant gate is run on the delivered source closure after repair.
Two artifact-digest ledger rows also used an undeclared disposition spelling.
They now use the gate's existing `NOT_JDK_STRING_SEMANTICS` spelling; their
owners, reasons and exclusion from String runtime state remain unchanged. The
gate's allowed dispositions and runtime rejection rules are unchanged.

The Java probe is generated into jtreg by the recipe. Local Maven tests compile
the frozen target preimage and candidate in separate java.base patches using
Java 21, `-proc:none -implicit:none -Xlint:all -Werror`. The same protected
probe runs against stock Java, frozen target and candidate. It compares exact
observable results, captures and state through reset, regions, flags, pattern
changes, replacements, Unicode/surrogate boundaries and mutable inputs. Reuse
checks inspect fact identity separately. The old target fails the reuse control;
a deliberately broken invalidation path fails both identity and semantic checks.

All public/private Pattern and Matcher descriptors and fields are compared with
the frozen target. jdeps must report only java.base; its expected split-package
diagnostic is retained because this is a patch-module test. Runtime modes are
interpreter, mixed, C1 and non-tiered C2, with `-Xcheck:jni`. C1/C2 require an
actual compiled reset overloads and fact-consuming search in the compilation log.
C2 may inline the private gate into search. No native operation
is newly accelerated by MR.

Cold preparation and four warm batches are measured separately in two fixed
fixtures. The allocation receipts describe those fixtures, not a universal
throughput claim. They distinguish avoiding repeated preparation from the
remaining query work and retained metadata.

Reproduce with Java 21 and Maven 3.9.9:

```sh
bash m3/tooling/mr/verify.sh
# In an exclusively owned worktree, materialize through the recipe:
mvn -B -ntp -f m3/tooling/mr/pom.xml -Dmr.materialize=true test
```

The existing String workflow gains this recipe proof, a matching `make images`
step, and the generated jtreg test. Its earlier tests remain. A declared or queued
image job is not a pass. Local patch-module evidence does not prove the forked
M3 String runtime, HotSpot, JNI, GC, CDS, serviceability, all platforms, complete
reactor, coverage, JCK or whole-repository admission. Those gates remain open.

`evidence/receipt.json` records the exact executed boundary. `history.json`
classifies the three relevant Matcher history atoms as SURVIVES. RXM/RXA source
was checked against the previous delivered Synexia postimages, not inferred
from merged PR ancestry. The existing challenge-category review remains algorithm
evidence only; no challenge-site implementation or new third-party runtime code
is copied by this increment.
