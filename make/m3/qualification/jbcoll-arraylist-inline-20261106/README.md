# jbcoll-arraylist-inline-20261106: census leaf L002, `java/util/ArrayList.java` inline deletion mask

This is M3JDK convergence census leaf L002 (java.base collections, lane owner com-synexia-45). It is based on master
`af319ee82d00ff43fa0d4b161ab325df2863c199`. The donor is hsoliwal/com.synexia develop
`ca1aafc4d94f9dd1ee288226dcca070cc60991c9`, crate `m3jdk21-java-base-arraylist-inline-20261025`.

**Verdict: applied.** The crate hunk now lands in `ArrayList.removeIf(Predicate, int, int)`, and the crate test moves
to `test/jdk/java/util/ArrayList/M3ArrayListInlineMaskTest.java`. The change is an internal performance shape:
- Spans of up to 2 x 64 elements keep their deletion decisions in two `long` locals. For these spans `removeIf`
  allocates no `long[]` deathRow.
- Larger spans keep the original deathRow layout, and spans of 512 or more still go through `M3CollectionMaskCompactor`.
- Observable behaviour is identical to stock JDK 21 and to master. The differential below shows this byte for byte.

## Census rows (CRATE_STATUS.tsv, re-run at master cf0429ac1d49: unchanged)

| path | postimage blob (sha256) | status |
|---|---|---|
| `src/java.base/share/classes/java/util/ArrayList.java` | `a03fee656f90` (`62d73bd98166`) | UNAPPLIED, added_present=0/19, 1 hunk, 37 changed lines |
| `test/jdk/java/util/ArrayList/M3ArrayListInlineMaskTest.java` | `56ce4a884caf` (`f4ed9e0a82a7`) | NO_OWNER (new file) |
| `src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java` | `51ea3a72bb19` (`1c3f3d2cf129`) | census leaf L003, not this leaf |

Why the merge is clean:
- The crate preimage `pre/ArrayList.java.txt` is blob `885d891b0f81`. That is byte for byte the live owner blob on
  master.
- So the 3-way merge has base = ours, and its result is the crate postimage (theirs).
- `git log --since=2026-10-01` on the owner shows one commit, 336d70ac128 (2026-10-18). The crate is dated 2026-10-25,
  so its hunk was never landed or reverted.

## Recipe JBCOLL_ARRAYLIST_INLINE v1 (`apply.py`, `PLAN.tsv`, `DONOR.tsv`)

The recipe derives from `progress-monitor-unify-20261106/unify.py`. Its two rules:
- **EDIT:** three anchored substitutions, each required to occur exactly once. They were generated mechanically from
  the pinned pre/post blobs.
- **NEW:** the crate test, written from its pinned donor blob.

Hash pins and refusals:
- The owner postimage must equal the crate postimage plus one JDK naming rewrite (`PROSE_PRODUCT_VERSION`). The rule
  turns the crate's comment "M3JDK21:" into "M3JDK 21:". The final sha256 is `732324a2ad40`.
- `--donor-git` re-derives the postimage from the crate blobs and refuses on any difference.
- The recipe also refuses on drift. After the naming rewrite the on-disk owner no longer matched the pinned postimage,
  and the recipe refused, as designed.
- A second run reports `changed=0 plan=UNCHANGED`.

The test is already jtreg-shaped: it has `@test`, `@run main/othervm M3ArrayListInlineMaskTest`, and a plain `main`
in the default package. It needed no relocation edits.

## Differential (`ArrayListDifferential.java`; seed 0x4d334a42414c4931; 1,000,000 ops; each run under 5 s)

The driver uses a single seeded op stream over an ArrayList of identity-only elements. The op mix:
- `removeIf` on the whole list and on sub-lists, over 11 predicate patterns. These include positions 63/64/127/128/
  511/512, density, and runs.
- `removeAll` and `retainAll` on the list and on sub-lists.
- Predicates that throw, that add (which must raise CME), and that `set` during the walk.
- Iterator and sub-list epoch probes for ConcurrentModificationException timing.
- add, addAll, remove, and subList clear.

Each line records the result, the callback count, and a hash of callback identities in call order. It also records
the list's size and identity hash.

Results:

| run | result |
|---|---|
| stock JDK 21.0.9 vs master ArrayList (patched) vs leaf ArrayList (patched) | **byte-identical**, sha256 `a46431cbc3a20e33...` |
| coverage | removeIf spans: none 383,009 / inline (1..128) 98,421 / scalar (129..511) 14,878 / compactor (512+) 2,541; 15,026,875 callbacks; 52,340 CME outcomes |
| leaf under `-Xint` (200,000 ops) | identical to the stock prefix |
| mutant of the inline path (`ordinal <= Long.SIZE`) | 79,337 differing lines; `M3ArrayListInlineMaskTest` fails ("root size"). The patch is loaded and the differential is sensitive |

INFO, `InlineMaskAllocationProbe` (ThreadMXBean bytes for 100,000 rounds of `removeIf`):

| span | stock | master | leaf |
|---|---|---|---|
| 64 | 2,400,000 | 2,400,000 | **0** |
| 128 | 3,200,000 | 3,200,000 | **0** |
| 129 | 3,200,000 | 3,200,000 | **0** |
| 300 | 5,600,000 | 5,600,000 | 5,600,000 (deathRow kept) |

## Gates (GATES.tsv; `verify.sh`)

| gate | result |
|---|---|
| recipe | PASS. changed=0, donor=VERIFIED, plan=UNCHANGED |
| patch_compile | PASS. `javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<ArrayList + M3CollectionMaskCompactor>`: base and leaf, 8 classes each, **0 warnings** |
| differential | PASS. See the table above |
| tests | PASS. The new test plus `test/jdk/java/util/ArrayList` ran as plain mains under `--patch-module` (`tests.sh`). Leaf: pass=14 fail=0 skip=2. Master: the same. Skipped: `ArrayManagement` (testng, no framework jar on the host) and `Bug8146568` (`main/manual`, needs 17 GB) |
| naming | PASS. `jdk_naming.py` apply, then plan, over the 3 touched paths: 0/0/0 |
| scope | PASS. Only `ArrayList.java`, the new test, and this directory |

Not run: jtreg itself (no jtreg on the host).

Rerun: `JAVA_HOME=<JDK 21> bash verify.sh <com.synexia clone> ca1aafc4d94f9dd1ee288226dcca070cc60991c9 <scratch> 1000000 <base>`
