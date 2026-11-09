# Failure classifications and limits

## Baseline defects with admitted repairs

The preimage checks intentionally retain failing witnesses. They are distinct from failed qualification of the admitted receiver.

| Area | Preimage observation | Admitted repair and evidence |
| --- | --- | --- |
| Filtered sorted navigation | Cursor construction threw a caller-supplied `IllegalArgumentException` inside the range-recovery block, which could be swallowed | Construct cursors outside that block; preserve the exact exception object; candidate contract 1,346 checks and 1,008 unchanged successful traces |
| Filtered deque occurrence search | Equality used the stored element as receiver and disagreed with the query-first `Deque` contract for asymmetric equality | Use query-first equality, preserve visible occurrence/removal behavior; candidate 140,620 checks |
| Reentrant selected traversal | Callback reentry could corrupt the active selection state and produce `NullPointerException` | Save and restore per-call selection state; preserve current copy-on-write and predicate-composition owners |
| Versioned bitmap cache | Callback invalidation could be lost; the baseline witness expected six values and observed three | Preserve the invalidation generation across callback execution, without callback retry |
| Public identity views | Baseline filtered identity values could admit/remove an equal foreign reference; filtered identity set hashing used ordinary element hashes | Owner-explicit classification in `IdentityHashMap`, metadata-only known-wrapper unwrapping in `Collections`, and identity-aware public `CollectionViews`; 540 new identity checks plus 46 retained checks |

Before/after command and source receipts are bound by `RAW_EVIDENCE_SHA256.tsv`. The complete receiving catalogue and file-pass records explain each selected donor atom and preserved contract. The passing final owning and linked gates are separate from these deliberately failing preimage witnesses.

## Work and allocation witnesses

The baseline empty-selection compactor made five payload reads in the instrumented fixture; the admitted implementation makes zero while retaining its second state scan and drift check. These are native-work observations, not an assertion that an ordinary input previously returned a wrong result.

The one-million-row zero-survivor scalar admission probe observes 1,000,882 thread-allocated bytes per operation before the packed-selection atom and 125,882 after it on the boot JDK. The linked source-built VM observes 125,890. These numbers describe the specific thread-allocation probe and its VM, not deep retained heap or a universal memory reduction.

The radix diagnostic retains all tiny-input observations and the 65,536-row JNI varying-input CPU regression (candidate/baseline ratio 1.137). No universal speedup is claimed. Native scratch is modeled from source; process RSS includes unrelated JVM/GC/shared-page costs.

## Harness and preparation failures retained separately

| Attempt | Classification and disposition |
| --- | --- |
| First module compilation placed test classes in the production module output | Harness separation error triggered a missing-explicit-constructor warning. Separate production/test outputs fixed the harness; no product warning gate was relaxed. |
| Initial public-core compilation enabled a different `this-escape` warning policy | Baseline and candidate both failed. The final compile follows the existing `java.base` owner policy in `Java.gmk`, retains `-Werror`, and independently passes full protected Javadoc checks. |
| Initial legacy donor fixture lacked `--add-opens` | Harness access failure was corrected before deciding the fixture's relevance. |
| Old donor `CollectionViewsContract` expects private `plan`/`tail` fields | With the access setup corrected, baseline and candidate both reject that obsolete implementation assumption. The fixture was excluded. The retained current-owner `CollectionViewsCoreContract` passes 14,694 behavioral checks, including deep composition, branching/concurrency and allocation coverage. |
| Early views recipe preparation enumerated a fourteenth unlisted legacy test | The attempt was stopped without receiver application. The exact 13-file manifest was used to construct an isolated selection. |
| Subsequent local views preparation retained unused resources | The proposed `combine-views21` identity was retired. Fresh `combine-views-final21` has exactly 13 targets and 23 resources, passed its SDK/refusal gates, and was applied with check/apply/replay counts 13/13/0. Bytes and mtimes were stable on replay. |
| First full JDK image build rejected 29 missing public-factory Javadoc tags | The seven `CollectionViews` factory declarations received the donor's correct type/parameter/return tags. The source build retained warnings-as-errors. The later passing source-image build is separate integration evidence. |
| First paired resource JNI process omitted `m3.collections.signal.native=true` | The existing owner correctly refused the missing opt-in. That process exited one. Only the harness property changed. All eight processes were rerun; the initial two Java observations and failed JNI process are excluded from the final 96 rows and retained as a separately hashed attempt. |

## Explicit remaining boundaries

The integrated public `java.util` identity repair does not repair the independent generic `com.m3` proving-ground identity-view defects. No heuristic based on JDK inner-class names and no reflective ownership bridge were introduced. Bulk `removeAll`/`retainAll` keeps the original operand-`contains` direction, and wrapper traversal/mutation stays on the original backing wrapper.

The navigation repair covers cursor construction. Comparator exceptions thrown while constructing a bounded range are still subject to the existing range-recovery behavior; this packet does not claim to distinguish those from a legitimate out-of-range bound.

The tests do not promote the global String-to-arrays-to-collections admission phase. They do not establish all-provider coverage, all platforms, full jtreg/JCK, every JMM/GC scenario, native allocation peaks, or universal performance. The linked image has 29 selected modules, and its external test harness is not part of the installed production module.

All installed-image results in this packet are E1 results for the 49-file checkpoint and exact binary hashes in `EPOCHS.json`. Later String-only successors require fresh source-image and relink evidence. Reusing earlier collection test evidence must be labeled as reuse, with collection source/JAR/native seals unchanged; it must not be described as a newly executed 491-test run.
