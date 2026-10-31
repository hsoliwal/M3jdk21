# Composite String structure and consumer coverage

Baseline: `hsoliwal/M3jdk21` commit `a4415af418a9b0f21a06cd5801d238577fbb6eb7`. Paths below are repository-relative. This is a source evaluation and acceptance map; an existing implementation is not a claim that its runtime gates passed in this task. Candidate runtime and performance qualification are recorded separately.

## Contract and implemented increment

Public `String` continues to define immutable UTF-16 code-unit equality, Java's 31-based hash, and lexicographic `compareTo`. Canonical payload identity is sufficient only for the already existing exact same-coordinate equality gate; distinct owners, segmentation, and equal hashes still require exact text comparison. String object identity, weak references, custom comparator authority and mutable collection callbacks are unchanged.

The candidate changes two existing owners. `M3String.CodeUnitCursor` caches the complete clipped scalar interval and continues forward with parallel dynamic owner/coordinate arrays; arbitrary or backward transitions retain root seeking. The arrays are allocated only after requesting a second adjacent scalar run. Popped and abandoned owner slots are cleared. `M3String.contentEquals` and `M3StringPrecomputeBridge.compareTo` use this reader only for tuple-backed operands, preserving the original scalar/flat loops and all pre-existing equality gates.

This new traversal metadata has no per-entry objects and no text/native-address copy. Existing retained tuple nodes and cache entries are listed explicitly below; this recipe does not claim to convert the whole owner graph to array storage.

## Structure choices: existing, implemented, or pending

| Structure / proposal | Existing owner and evidence | Disposition in this increment |
| --- | --- | --- |
| Immutable payload atom, packed range | `src/java.base/share/classes/java/lang/M3String.java`, `M3StringAtom.java`, `M3StringImmediate.java`; value retains owner plus packed coordinate, atoms retain payload ownership | Reused; no second string store or payload wrapper layer |
| Small-value immediate encoding | `M3StringImmediate.java` has packed Latin-1/identifier lanes and exact text check | Existing fast paths preserved; no cursor stack for scalar owners |
| Balanced persistent concatenation tree | `M3StringPool.java` `concatBalanced` / `balance`; `M3StringTuple.java` left/right references, height and composable facts | Existing owner reused; no new rope. Retained tuple nodes are still objects, so a full array-index DAG migration remains pending |
| Sequential piece list / temporary traversal stack | `M3StringPool.LeafCursor` has parallel owners and packed coordinates; `M3StringBuilderLane` has `String[]`, `long[]`, `int[]` lanes | Reuse the leaf cursor discipline in `CodeUnitCursor`; temporary storage is depth-bounded and does not retain abandoned owners |
| Full visible leaf interval | Open PR 1456 head `b2d03f8fccfdce93696e6162246b6f84b998f7f9` already proposes complete clipped scalar caching | Preserve that semantics; distinct new work is forward continuation between leaves, plus equality/natural-order consumers |
| Wide B+ tree / RRB / piece-table representation | No generic adaptive owner family received in the reviewed String classes | Pending workload and representation-budget decision. Do not install all structures on every String |
| Masks, runs, bitmap and rank/select | `M3StringProjection.java`, `M3StringRepresentationPlan.java`: RANGE/RUNS/BITMAP metadata selection by actual geometry/storage size | Existing selection view reused; not a generic tree shape selector and not public String replacement |
| Compact integer/bit/run lanes | `src/java.base/share/classes/jdk/internal/mindex/M3IntLane28.java`, `M3LongLane28.java`, `M3BitLane28.java`, `M3RunTable.java` | Existing lower-level index owners; parent inventory covers mapping/lifetime integration |
| Composable hash, lengths, UTF-16/UTF-8 facts | `M3StringFacts.java`, `M3StringOwner.java`, `M3StringTuple.java` | Existing exact summaries preserved. Boundary adjustment remains necessary for split surrogate pairs |
| Range/owner caching | `M3StringOwner.java` has four bounded range-fact slots; precompute caches key exact owner and coordinates | Existing scope retained; `RangeFact` and some precompute `Entry` objects remain in baseline and are not silently described as array-only |
| KMP, reverse KMP, BMH, trigram absence filters | `M3StringSearchPrecompute.java` with bounded pattern/source cache sizes and exact fallback | Existing cursor consumers receive the new reader. Filters remain negative gates, not equality or match proofs |
| Frozen lexicon index | `M3FrozenLexiconIndex.java` displacement arrays select hash/length candidates followed by exact UTF-16 check | Existing hash index, not a trie. Do not add duplicate dictionary ownership |
| Trie / prefix routing / suffix array / suffix automaton | No universal per-String implementation in reviewed owner files; collection trie scaffolds tracked separately | Optional query-owned workload indexes remain pending; materialization, retained bytes, invalidation/ownership and amortization must be measured first |
| Regex summaries and DP rows | `M3StringRegexDp.java`, `M3StringRegexRowPrecompute.java`, `java/util/regex/M3PatternQuery.java` | Existing admitted pattern subsets and fallbacks preserved. No claim of a finite universal summary for captures, backreferences, lookaround or observable Matcher state |
| JNI flat shadows and byte/char exports | `src/java.base/share/native/libjava/String.c`, `M3StringPool.c`, `jni_util.c`; Java bulk copy owners | Parent owns range/overflow repair and matched JNI/VM gates; this cursor retains no raw address |
| Pressure, spill, persistence, wider cache replacement | Existing local/mapped atom ownership and bounded precomputes | No new async spill pipeline, global memo table or automatic structure migration in this increment; requires separate resource/lifetime evidence |

## Per-file Java owner evaluation

The following rows evaluate every `java.lang` or regex production file from the baseline `rg -l 'M3String|m3_string|m3String' src` inventory, plus the frozen lexicon index. The parent inventories the native/HotSpot/serviceability and `jdk.internal.mindex` rows from the same search. A search hit alone is not treated as a completed integration.

| Production file | Evaluated responsibility | Candidate decision and relevant gate |
| --- | --- | --- |
| `src/java.base/share/classes/java/lang/M3String.java` | Canonical range, composition, operations, streams and logical UTF-16 cursor | Changed existing cursor and tuple equality only; new continuation test exercises complete clipping, reverse/random seeks, seam surrogates and owner-stack clearing |
| `src/java.base/share/classes/java/lang/M3StringPrecomputeBridge.java` | Public String dispatch into canonical operations | Changed tuple natural-order loop only; exact char difference and prefix-length result retained; existing same-coordinate equality/order gate retained |
| `src/java.base/share/classes/java/lang/M3StringOwner.java` | Owner metadata, exact/full and bounded range facts | Unchanged; no new unbounded owner cache or altered publication/lifetime |
| `src/java.base/share/classes/java/lang/M3StringImmediate.java` | Small scalar packed payload | Unchanged; existing immediate equality before cursor decision remains authoritative |
| `src/java.base/share/classes/java/lang/M3StringAtom.java` | Local/mapped immutable scalar payload, native reads and copying | Unchanged; reader calls owner methods and keeps owning Java references, preserving fences |
| `src/java.base/share/classes/java/lang/M3StringTuple.java` | Persistent concatenation, lengths, height, facts and scalar access | Unchanged retained representation; candidate avoids repeated tuple descent during forward scans |
| `src/java.base/share/classes/java/lang/M3StringPool.java` | Weak canonicalization, budgeted admission, balanced concat, existing leaf cursor | Reused algorithm owner, not replaced; arrays carry owner/range handles rather than new entry objects |
| `src/java.base/share/classes/java/lang/M3StringFacts.java` | Exact composable summaries with UTF-16 boundaries | Unchanged; hash and surrogate seam contracts remain covered by existing facts tests |
| `src/java.base/share/classes/java/lang/M3StringRepresentationPlan.java` | Range/run/bitmap selection geometry | Unchanged; selection thresholds are distinct from cursor and tuple balancing |
| `src/java.base/share/classes/java/lang/M3StringProjection.java` | Immutable source-coordinate selection view | Unchanged; projected geometry, nested selections and rank/select require their existing dedicated test, not inferred from tuple cursor proof |
| `src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java` | Bounded exact literal search plans and trigram negative gates | Existing cursor call sites inherit continuation; KMP backtracking and reverse plans require arbitrary/reverse seek correctness |
| `src/java.base/share/classes/java/lang/M3StringAffixPrecompute.java` | Bounded prefix/suffix summaries and exact verification | Unchanged; no speculative affix cursor conversion before measured benefit |
| `src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java` | Range/position operation metadata | Unchanged; do not add a universal index to each owner |
| `src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java` | Code-point rank and surrogate-aware geometry | Existing cursor traversal inherits change; UTF-16 unit positions remain distinct from code-point count |
| `src/java.base/share/classes/java/lang/M3StringRegexDp.java` | Admitted exact regex DP evaluation | Existing cursor use inherits change; parsing/admission/observability guards are unchanged |
| `src/java.base/share/classes/java/lang/M3StringRegexRowPrecompute.java` | Derived regex row reuse | Unchanged; row ownership and bounded budgets stay operation-specific |
| `src/java.base/share/classes/java/lang/M3StringBuilderLane.java` | Mutable-builder immutable-piece lane, 64-unit lift and pin-ratio threshold | Unchanged; parallel piece/coordinate arrays compose through existing pool; no premature flattening added |
| `src/java.base/share/classes/java/lang/AbstractStringBuilder.java` | Append/copy/mutation/capacity boundaries and builder lane | Unchanged; mutable source synchronization, callback order and detachment cannot be bypassed by immutable String shortcuts |
| `src/java.base/share/classes/java/lang/String.java` | Public equality/hash/ordering/search, ingress, streams and exported representations | Already routes equals/hashCode/compareTo to existing owners; no public method, ABI, String identity or hash behavior changed |
| `src/java.base/share/classes/java/lang/StringConcatHelper.java` | Concatenation converted pieces and canonical append | Unchanged; reference concatenation supplies the tuple workload without another storage layer |
| `src/java.base/share/classes/java/lang/invoke/StringConcatFactory.java` | Invokedynamic concat and stringifier invocation | Unchanged; user conversions/effects remain in existing order |
| `src/java.base/share/classes/java/lang/System.java` | Post-bootstrap activation and JavaLangAccess prepared trigram bridge | Unchanged; runtime activation and fallback remain under current experimental gate |
| `src/java.base/share/classes/java/util/regex/M3PatternQuery.java` | Conservative query derivation from JDK compiled nodes, bounded Boolean subset | Unchanged; no alternate parser and no replacement for Matcher observable semantics |
| `src/java.base/share/classes/java/lang/M3FrozenLexiconIndex.java` | Primitive displacement/row index for immutable lexicon | Unchanged; exact collision check remains necessary |

## String-key collections and other consumers

All paths in this table are under `src/java.base/share/classes/`. The candidate improves the String operations these callers already invoke; it does not rewrite collection key semantics or store canonical payload identifiers as keys.

| Consumer family / exact owner paths | Existing route | Candidate coverage / limitation |
| --- | --- | --- |
| `java/util/HashMap.java`, `LinkedHashMap.java`, `HashSet.java`, `LinkedHashSet.java` | Java hash plus exact String.equals; tree bins may invoke natural compareTo | New keyed fixture covers 64 colliding distinct spellings, equal strings with different segmentation, insertion order, duplicate insertion, lookup/replacement/removal and computeIfAbsent callback preservation |
| `java/util/TreeMap.java`, `TreeSet.java` | Natural String.compareTo or supplied comparator | New fixture covers exact UTF-16 order, prefix lengths, floor/ceiling/lower/higher; supplied comparator remains authoritative |
| `java/util/concurrent/ConcurrentHashMap.java`, `ConcurrentSkipListMap.java`, `ConcurrentSkipListSet.java` | Hash/equality or natural/supplied comparator under their existing concurrency protocols | New fixture covers key semantics; contention, linearizability and concurrent resource stress are separate unclaimed gates |
| `java/util/ImmutableCollections.java`, `Map.java`, `Set.java`, `Collections.java` | Immutable factory/copy exact keys and ordinary wrapper delegation | Collision lookup, duplicate rejection, singleton/unmodifiable cases in new fixture; no rewritten wrapper semantics |
| `java/util/Hashtable.java`, `Properties.java` | Existing hash/equality; Properties delegates to CHM | Legacy and String-property key lookup exercised; synchronization unchanged |
| `java/util/WeakHashMap.java` | Hash/equality with weak original key lifetime | Content comparisons benefit; new fixture keeps original keys strongly reachable. It does not claim GC reclamation proof and does not replace weak keys by immortal canonical identities |
| `java/util/IdentityHashMap.java` | String object identity | Explicitly excluded from value-equality optimization; equal differently segmented Strings remain two entries in the fixture |
| `java/util/stream/Collectors.java`, `DistinctOps.java`, `StreamSpliterators.java` | Default grouping/hash sets or supplied map factory; parallel distinct CHM path | Sequential groupingBy/distinct fixture; configured identity maps/custom comparators retain their contracts. Parallel-specific qualification remains pending |
| `java/util/Arrays.java`, `ComparableTimSort.java`, `TimSort.java`, `Collections.java` | Natural compareTo or supplied comparator for sort/search | Benefit inferred only for the exact natural route; no new sorting algorithm. Full sort/binary-search workload qualification is a follow-up |
| `java/lang/Class.java`, `ClassLoader.java`, `jdk/internal/loader/BuiltinClassLoader.java`, `Loader.java`, `LoaderPool.java` | Enum/class/module/package/resource maps keyed by names | Reach existing String hash/equality through actual maps. Bootstrap and class-loader stress are required separate gates; no code-generation or class-identity shortcut |
| `java/util/ListResourceBundle.java`, `PropertyResourceBundle.java`, `ResourceBundle.java` | String lookup maps plus resource cache keys | Basic String-key route identified; domain cache composite-key/locale logic stays unchanged |
| `java/util/jar/Manifest.java`, `Attributes.java`, `JarVerifier.java`; `java/util/zip/ZipOutputStream.java` | Manifest entry/signature maps, duplicate-name String set; attributes have case-insensitive Name semantics | Only ordinary String keys inherit comparison improvement. Attribute-name case folding, native ZIP byte-name indexes and security semantics are not replaced |
| `java/util/regex/Pattern.java`, `Matcher.java`; `java/util/PropertyPermission.java` | Named-group maps and permission-name map | Map String comparisons inherit exact behavior; regex/group/permission semantics remain their owners' responsibility |
| `src/jdk.zipfs/share/classes/jdk/nio/zipfs/ZipFileSystem.java`, `ZipFileAttributeView.java` (outside prefix above) | String environment/attribute maps and supported view names; inode index uses another key type | String map uses identified; do not label the inode/path index as a String-key optimization |
| Received `java/util/M3*` collections | Array-backed hash slots, tree indexes, shared-lock wrappers, bidirectional and multi-value maps | Companion catalogue `m3/collections/algorithm-catalog/string-collection-consumers-20261021/` records exact owners, supported vs scaffold APIs and excluded strategy/identity/weak paths |

## Proof and acceptance boundaries

The standalone extraction harness uses the exact baseline and candidate cursor bodies with independent heap-coordinate owners and char-array oracles. Its recorded final run passed 2,046,846 checks; a 4,096-piece forward scan counted 53,248 baseline owner resolutions and 8,203 candidate owner resolutions. This is traversal-model evidence, not elapsed performance, Java-base execution, native lifetime or GC qualification.

`test/jdk/java/lang/String/M3StringCursorContinuationTest.java` and `M3StringKeyedCollectionsTest.java` compile on JDK 21 with `-Xlint:all -Werror`. The keyed fixture's optional stock-reference mode passed 17,575 checks and explicitly reports `referenceOnly=true`; it does not execute the candidate. The matched M3 VM must run both target test mains with CompactStrings on and off. Existing literal-search, streams, facts, mixed-equality-gate, precompute-bridge, owner-range-lifetime and JNI-shadow tests remain relevant gates.

The timing harness separately compares early/late hash collisions and natural ordering on the same source-built baseline and candidate images. It reports raw per-sample nanoseconds and checksums with no success threshold. It includes scalar controls and asserts actual tuple/scalar distinct owners in M3 mode. Root's recorded image/build identity and same flags are required before interpreting the samples.

Pending scope remains visible: retained tuple/cache object migration, wider structures, special-purpose trie/suffix indexes, unrelated operation loops, concurrency/GC/resource stress, compiled VM configurations and all source-built integration gates. This increment makes no claim that a broad source inventory or a passing host model completes those items.

The companion timing source is received at
`test/jdk/java/lang/String/M3StringKeyedTiming.java`, preserving its exact source
bytes while using the existing typed Java test source root.
