# Deep filtered/sorted wrapper composition

This is a bounded refinement of **existing** M3 collection owners, not another
collection API/storage hierarchy. Parent: M3JDK21 #996, exact commit
`d9fe52e42a04515725e85e3c5c68ac185adbcd44`. The inherited filters, mutable sorted
ranges, navigable operations, explicit read-only sorted snapshots, and Map entry
guards remain authoritative. Other keyed execution/purpose/primitive/counting
PRs are separate owners and are not implicitly merged by this change.

## Defect, implementation and preserved contract

Both old constructors flatten the backing wrapper, but compose a recursive
`previous.test(...) && next.test(...)` lambda. Actual original collection and map
sources overflow at 65,536 filters with `-Xss256k`. Map lambdas also capture the
prior wrapper. The refined constructors share a package-local predicate atom in
`M3CollectionViews`: a persistent binary-carry tree of predicate **occurrences**.
Existing completed subtrees are reused. No new public API is added.

* Evaluate leaves left-to-right; stop at the first false or thrown exception.
  Repeated identical predicate objects still execute at every occurrence. No
  speculative, parallel, deduplicated, cached, or reordered client callbacks.
* Retain the original backing owner, comparator, visible-only mutation, null
  policy, guarded entries, range/descending navigation, encounter order, lazy
  unsized traversal and snapshot behavior. Old prefix views remain independent
  of newly appended predicates. Java predicates remain opaque callbacks.
* Wrapper-created predicate call depth is O(log d), for d filter occurrences;
  evaluation visits O(d) leaves in the worst case. Append takes O(log d) path
  copying and a final plan retains O(d) predicate metadata, not element payloads.
  Traversal uses logarithmic JVM stack frames, **no per-element heap scratch**.
* This allocates more construction metadata than the former single lambda per
  append. No universal allocation/heap/throughput superiority or JMH claim.
  Java recursion inside a caller's own predicate remains outside this guarantee.
  There is no imposed 65,536-filter API limit; that is a tested depth.
* Thread-safe immutable plan sharing does not make mutable owners or callbacks
  thread-safe. Preserve the existing stable/noninterfering predicate contract and
  use an external common lock when required. No cross-call scheduling changed.

Existing usage remains unchanged:

```java
Collection<Order> visible = M3CollectionViews.filtered(orders, Order::isOpen);
visible = M3CollectionViews.filtered(visible, order -> order.customerId() == customer);
List<Order> sortedCopy = M3CollectionViews.sortedSnapshot(visible, byDate);
NavigableSet<Long> liveIds = M3CollectionViews.filtered(ids, id -> id >= 0);
NavigableMap<String, Order> liveMap = M3FilteredMaps.filtered(index, (key, order) -> order.isOpen());
```

A sorted snapshot is detached and duplicate-preserving; a filtered sorted wrapper
is live and preserves its owner's comparator/ranges. The public methods above
are **reused**, not newly invented overloads with changed dispatch.

## Recipe-first custody and execution boundary

Named recipe `com.synexia.m3jdk.DeepFilterComposition20261017` composes the retained
`M3HashPinnedJavaSnapshotRecipe` and `M3HashPinnedTextSnapshotRecipe` in the
**existing** `m3/collections/wrappers/recipe` Maven module. Five Java targets and
one C target have exact pre/post SHA-256 seals; two owners are modified and four
test targets are initially absent. The SDK test validates real named-recipe
Results, Java LST mode, six single-target partial deliveries, fixed point,
missing/drift/occupied/non-Java/duplicate refusal and unrelated preservation.
Optional SDK export runs only after every assertion in its single JUnit method.

The offline application path **reuses Synexia's existing Java/Maven `Replay`
adapter**, not a new transformer. Its exact original Git blob and all three
literal bindings (crate path, manifest digest, M3 target prefix) are retained.
No parser/transformation/locking/staging method is rewritten. The original log
label `M3 short flag replay` is intentionally retained to avoid altering its
logic; the manifest and receipt identify this task. The subordinate Maven POM
exposes the same adapter with explicit source/target/mode properties.

**Executed locally:** actual bound Java replay adapter, complete preflight before
writes, per-file staged application, nonmutating refusal cases, and no-write
fixed point. Java/JNI proofs compile and execute the adapter's six real emitted
files, not hand-edited outputs. **Not executed locally:** Maven executable,
OpenRewrite SDK/JUnit, full owning reactor, or source-built JDK. Do not relabel
Java adapter execution as Maven/OpenRewrite SDK execution. The draft remains on
hold for independent exact-head SDK/receiver and broader product gates.

The adapter accepts all-PRE or all-POST and refuses mixed states. The canonical
SDK additionally supports admitted partial delivery. Filesystem publication is
atomic **per file**, not a repository transaction. An interrupted mixed result
requires reconciliation. Exclusive workspace ownership/cooperating file locks
are required; this does not defend against a malicious filesystem actor.

## JNI: reuse rather than a native predicate engine

The new C source is a **test-only forwarding symbol** calling the existing,
byte-unchanged `m3_map_cursor_jni.c` primitive region kernel. Java handles all
predicates, comparators, references and sorting. A detached primitive shadow is
passed to native code once for the fold; no heap address, native handle or
callback persists. No production native symbol changes or acceleration claims.
The existing C source's Git blob is checked before compiling under C17, strict
warnings, fatal UBSan and checked JNI. Linux x86-64 is the executed native target.

## Reproduce

From this source checkout, with Java21/Python3/GCC on Linux and a **fresh absent**
output path (the checkout is not modified):

```sh
python3 m3/collections/wrappers/deep-filter-20261017/verify.py \
  --root . --output /tmp/m3-deep-filter-fresh
```

Real Maven/OpenRewrite qualification and optional actual Results export:

```sh
mvn -B -ntp -f m3/collections/wrappers/recipe/pom.xml test \
  -Dtest=M3DeepFilterRecipeTest \
  -Dm3.deep.sdk.output=/tmp/m3-deep-sdk-fresh
```

The Java replay adapter also has a Maven entry, requiring explicit source and
already-existing target roots. `--check` is the safe default; **qualify first**.

```sh
mvn -B -ntp -f m3/collections/wrappers/deep-filter-20261017/replay/pom.xml verify \
  -Dm3.replay.source=/absolute/checkout -Dm3.replay.target=/absolute/candidate \
  -Dm3.replay.mode=--check
```

## Actual finite evidence

The fresh successful verifier (`qualified-2`) executed **29 commands**, **13
admission refusals**, six target emissions, byte/mtime fixed point and two
identical public `javap -public -s` comparisons. Generated-source tests passed
**1,312,160 checks at depth65,536** (tiered JVM), **165,280 at depth8,192** (`-Xint`),
each with192 randomized seeds. Counts include structural invariant checks and
are not counts of distinct test methods. The actual JNI suite passed156 checks,
including depth65,536 sorted Set/Map projections, hidden survivors and native
array/null boundaries. Exact old source failed both intended overflow controls.

An earlier aggregate command timed out while running the larger interpreter
profile; its incomplete run is **not** counted as passing. The successful fresh
verifier retains full-depth tiered coverage and separately reports the smaller
interpreter depth. Neither timeout nor queued/zero-step hosted runs establishes
a source/compiler failure. Successful proof logs, excerpted original failure controls and receipts accompany
this PR. The downloadable packet also retains all 29 complete command logs in
`local-evidence.tar.gz`; each full log digest is in `LOCAL_RECEIPT.json`.

Still separate: actual Maven/SDK/JUnit execution, complete inherited provider and
module/reactor suites, serialization/GC/concurrency, other native platforms,
performance measurements, `java.base`/HotSpot/jtreg/JCK, and convergence of all
other keyed/primitive/counted collection PRs. No default/parent branch updates,
merge, rebase, force push or phase promotion is performed.
