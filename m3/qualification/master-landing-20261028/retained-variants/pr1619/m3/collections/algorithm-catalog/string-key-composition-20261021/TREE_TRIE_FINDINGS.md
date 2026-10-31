# String tree and trie findings at the receiving source

All M3JDK21 source facts below are pinned to
`18213fe70c66567fdfc6ba6f669909e7e9251663` (PR #1657).
The authored consumer tests and their runtime evidence are a separate layer.
The four-file filtered-comparator change is frozen under `candidates/views`;
this tree extension changes no production tree or trie implementation.

## Confirmed owners and boundaries

| Owner | Confirmed implementation | String relevance and qualification boundary |
| --- | --- | --- |
| [`M3TreeMap`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/src/main/java/com/m3/collections/M3TreeMap.java) | Operational AVL tree. Its `Tree` stores Object key/value lanes, int left/right/parent links, byte heights, root/free/used/size state. Natural comparison invokes Comparable.compareTo; an explicit Comparator owns custom ordering. Views share that tree. | Ordinary String keys therefore reach the integrated public String comparison path. The owner does not extract M3String coordinates itself. Primary topology is in arrays; public entry-facade behavior is separate. |
| [`M3TreeSet`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/src/main/java/com/m3/collections/M3TreeSet.java) | Operational key-only M3TreeMap. Range and descending sets use backed map views. | Can consume composed String keys through their ordinary API, including comparator equivalence. |
| [`M3PatriciaTrie`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/src/main/java/com/m3/collections/M3PatriciaTrie.java) | Explicit SCAFFOLD. Public operational atoms throw M3NotYetReceived. Object key/value, int bit-index/left/right and state-mask fields are declarations. | String keys and live prefixMap are declared, but no insertion, branch split, prefix traversal, deletion, terminal state or liveness implementation is established. OCCUPIED/DELETED/LEAF bits are planned state only. |
| [`M3LongPatriciaTrie`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/src/main/java/com/m3/collections/M3LongPatriciaTrie.java) | Explicit SCAFFOLD, with primitive long payload lanes and throwing operational atoms. | A primitive-long key domain is distinct from a String owner/range trie; the class declaration qualifies neither. |
| [`apache.M3PatriciaTrie` provider mirror](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/implementations/apache-patricia-trie/src/main/java/com/m3/collections/apache/M3PatriciaTrie.java) | Generated provider API surface; constructor and methods throw UnsupportedOperationException. | It is not the operational relocated Apache donor and not a completed provider receipt. |
| [`apache.trie.PatriciaTrie` relocated Commons donor](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/implementations/commons-collections-4.5.0/src/main/java/com/m3/collections/apache/trie/PatriciaTrie.java) | Operational Patricia algorithm supplied by AbstractPatriciaTrie. Actual TrieEntry objects retain parent/left/right/predecessor nodes, bit index and key/value fields. | Useful provider behavioral oracle and composed String consumer. This retained-node store does not qualify the target array/offset/mask trie. |
| [`M3SignalOrder`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/src/main/java/com/m3/collections/M3SignalOrder.java) | Primitive unsigned-key stable LSD radix sorting and its JNI counterpart. | Prior radix-stability evidence belongs to sorting, not to a radix-tree data structure. |
| [`M3FrozenLexiconIndex`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/src/java.base/share/classes/java/lang/M3FrozenLexiconIndex.java) | Immutable displacement hash index over Java hash plus UTF-16 length, with collision-group markers and exact checks owned by the pool. | It does not provide trie prefix traversal or terminal-mask ownership. |

## Donor semantics that must remain distinct

[`StringKeyAnalyzer`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/implementations/commons-collections-4.5.0/src/main/java/com/m3/collections/apache/trie/analyzer/StringKeyAnalyzer.java)
uses 16 bits per Java char, String.length/charAt for bit navigation, and
substring/startsWith for its prefix predicate. Thus a surrogate code unit is a
legal prefix boundary; code-point or normalized/collation order must not be
silently substituted. The default KeyAnalyzer ordering ultimately calls
Comparable.compareTo. The generic array AVL tree instead accepts an arbitrary
user Comparator; such comparator equivalence cannot automatically become a
bitwise prefix rule.

[`AbstractPatriciaTrie`](https://github.com/hsoliwal/M3jdk21/blob/18213fe70c66567fdfc6ba6f669909e7e9251663/m3/collections/implementations/commons-collections-4.5.0/src/main/java/com/m3/collections/apache/trie/AbstractPatriciaTrie.java)
uses `setKeyValue` when replacing an existing key, so its retained key reference
is replaced by the new equal key. M3TreeMap retains the original representative
on comparator-equal replacement. The donor proof asserts its own rule, without
projecting TreeMap semantics onto Commons. Prefix views are live and writes are
range checked. The proof exercises empty prefix, common paths, boundaries inside
String pieces, split surrogate prefixes, insertion of shared-prefix siblings,
replacement, deletion and prefix-view clear.

The unchanged donor cannot compile directly as Java21 source: package-private
`firstEntry`/`lastEntry` in AbstractPatriciaTrie conflict with the public
SequencedMap methods. Its own pom declares Java8 source and target. The evidence
therefore retains that Java21-source refusal, compiles unchanged source with
`--release 8` and its visible existing serial warnings, and compiles the new
consumer proof separately with Java21 `-Xlint:all -Werror`. Running that binary
on Java21 is limited donor compatibility evidence, not direct source receipt or
complete support for Java21 sequenced APIs.

## Executed author evidence

The verifier obtains every baseline source byte through git show at the pinned
commit, independent of later recipe output staged in the receiving worktree.
It compiles the actual 25-file tree dependency closure and the actual 15-file
Commons donor closure; it supplies no dependency stubs.

| Check | Observed result |
| --- | --- |
| New existing-tree contract on exact baseline | Expected failure: generic filtered M3TreeSet loses comparator alias membership. |
| Same contract with frozen comparator candidate, Java21 | Pass, 268,630 checks. |
| New module JUnit adapter | 1 test, 1 pass. |
| Explicit requireM3 mode on stock Java21 | Expected NoSuchFieldException for String.m3; it does not silently skip. |
| Tree contract on prior M3 source-built VM, compact and wide | Pass, 268,781 checks in each mode. Reflection verifies real String.m3 bodies, at least one tuple owner, and unchanged owner/range coordinates. |
| Unchanged Commons donor source as Java21 | Expected source-visibility refusal recorded. |
| Donor proof on Java21 over donor's original release8 bytecode | Pass, 31,247 checks. |
| Donor proof on prior M3 source-built VM, compact and wide | Pass, 31,250 checks in each mode, including actual tuple owner/range verification. |

The M3 runtime used is the pre-existing read-only image at
`/workspace/scratch/81117b672ae3/work/M3jdk21/build/m3-composed-release/jdk`.
These are interpreter runs with UseM3StringStorage enabled. Its launcher and
release-file hashes are recorded in SOURCE_RECEIPT.json. This does not establish
that the newly receiving product has been rebuilt, nor JIT/JNI, allocation,
performance or whole-provider-family acceptance.

The new emit targets are two module tests and one isolated donor qualification
proof. Contract, input SHA256 seals, output SHA256 seals and exact command logs
are under `candidates/tree-consumers` and `evidence/tree-consumers`. Canonical
Synexia emission and post-emission qualification remain owned by the recipe agent.

## Bounded existing-Synexia and catalogue inventory

Filename and exact-name source search covered the current M3JDK21 repository,
including `m3/vendor/synexia`, `m3/tooling`, the primary collection sources and
provider trees, plus the parent-provisioned local canonical host snapshot at
`work/synexia-recipe`. No operational MIndexPatriciaTrie, IndexStringTrie,
MIndexStringTrie, MIndexRadixTree, StringRadixMap or M3RadixTree declaration was
established by that search. The only matching M3 Patricia recipe outputs there
were the two five-library scaffold templates. The local canonical host snapshot
contains only selected files; this is not a claim that no suitable donor exists
elsewhere in Synexia or in another remote branch.

Pinned existing catalogue evidence agrees with the source boundary:

- `qualification/five-library-scaffold-20261018/SCAFFOLD_LEDGER.tsv` labels both
  primary Patricia owners SCAFFOLD.
- `qualification/five-provider-inventory-20261018/INVENTORY.tsv` identifies the
  apache-patricia-trie provider scaffold and M3TreeMap as a reuse candidate.
- The same inventory's STATUS_OVERLAY.tsv keeps PatriciaTrie OPEN.
- `relocation/five-provider-20261018/file-census/APACHE_INVENTORY.tsv` and
  ACTION_QUEUE_PER_FILE.tsv mark the AbstractPatriciaTrie/PatriciaTrie mapping to
  M3TreeMap as RECIPE_CANDIDATE_NOT_APPLIED.
- `receiving/commons-collections-4.5.0-20261018/FULL_SOURCE_BINDINGS.tsv` seals the
  relocated donor files; relocation alone does not apply that candidate mapping.

Exact matching catalogue rows and input hashes are retained in
`catalogue-source-receipt.json`; exact bounded search commands and outputs are in
`bounded-synexia-inventory.json`.

## Concrete remaining scope

An array trie can in principle use existing reusable slot allocation, child
indexes and String owner/range edge coordinates. A compressed-edge variant would
need tested terminal state independent of child presence, prefix matches ending
inside an edge, split/merge/deletion rules, retained-key and value semantics,
live view bounds, iterator/callback contracts and provider-specific ordering.
Those are requirements for a future implementation and receipt; the scaffold
fields and this finite donor proof do not establish them. The completed work
here strengthens the implemented tree/view consumers and makes that remaining
trie work explicit without creating a second unqualified public owner.
