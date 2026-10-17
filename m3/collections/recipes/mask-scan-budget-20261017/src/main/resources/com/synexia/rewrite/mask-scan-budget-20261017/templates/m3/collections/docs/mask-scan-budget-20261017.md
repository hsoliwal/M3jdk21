# Work-budgeted mask windows over existing ordered indexes

The additive `M3IndexMaskWindow.scanBounded` method bounds both output and input work.
The original `scan` signature and public method body remain unchanged. Its full remainder
search is still available. This is not a new collection store, viewport, renderer or live
predicate wrapper. `M3IndexMaskCompactor` is reused unchanged for backend availability.

## Contract

`maxExamined` is 0..4096 and `limit` is 0..4096. Each call returns the existing packed
`nextIndex`/`written` pair. A positive source/output budget consumes at most `maxExamined`
logical source positions and stops earlier when output fills or input ends. A page with
zero matches can still advance its cursor: **do not mistake an empty page for exhaustion**.
Continue until the cursor equals the source length. Zero output or source budgets do not
advance. Budget counts logical advancement; constant masks can skip actual array reads.

State bytes are aligned with ordered-index positions, not with physical row IDs. Required
bits must all be set and excluded bits all clear, using unsigned-byte interpretation.
Duplicate IDs and the input's sorted/encounter ordering are preserved; no sorting or
predicate evaluation is performed here. Both arrays stay caller-owned, unmodified and
unretained. Only the selected output interval is written. Output/source aliasing is refused.

Validation and the optional read-only cancellation checkpoint occur before writes. Pass
`monitor::isCanceled` or null; thread interruption is honored independently. Validation
and backend availability precede the checkpoint. The callback runs once, outside JNI.
Mid-call cancellation is observed at the next bounded call. Cancellation/validation
refusal leaves output unchanged; VM/native failures after writing begins are not claimed
transactional. The caller owns source stability and parent revision checks between calls.
No stale-source detection is invented for ordinary mutable arrays.

## Java and JNI reuse

The existing Java scan loop is a shared end-bounded private atom; the original scan uses
the complete array end. The same native C source serves both exports, using the incumbent
128-element JNI Region transfers and mask-first payload loading. Region read-ahead cannot
cross the admitted source budget. New JNI symbol: `M3IndexMaskWindow_scanBoundedNative`.
The original `scanNative` symbol remains and uses an internal unbounded sentinel.

NATIVE_REQUIRED never silently substitutes Java. Missing libraries refuse, including zero
work. With a legacy window binary, nonzero requested budgets require the new symbol and
refuse if absent. Existing native scans remain usable with that old binary. This is an
isolated mask-window ABI extension, not a claim that every optional native module is built.
The test runner's .so is a scoped qualification image: never install it over a complete
m3_collections_mask distribution.

Cost: O(min(maxExamined, remaining input)) primitive work per call and bounded output;
no per-row wrapper, result allocation or retained cursor. This is an operation bound,
NOT a wall-clock deadline or general native speedup claim. Paging changes the per-call
schedule; the total search still depends on total input length and selectivity.

## Usage and verification

See `examples/MaskBudgetExample.java`: ordered indexes plus high-bit state flags, output
offset, empty page, repeated resume, Java and explicit JNI. The caller may yield or recheck
its owner generation between pages; this API does not create a scheduler or event queue.

```sh
python3 m3/collections/recipes/mask-scan-budget-20261017/verify.py
mvn -B -f m3/collections/recipes/mask-scan-budget-20261017/pom.xml clean verify
```

The Maven/OpenRewrite recipe is `com.synexia.m3.MaskScanBudget20261017`, extending the
existing source-sealed M3 recipe. Edit templates/recipe, qualify, regenerate, then inspect
exact postimages. No generated-output hand edits. The source pair is reviewed serially:
native source first (including the old ABI corpus), then the Java entry point, then tests,
example and catalogue. Recipe/test/evidence and result commits are separate for review.

## Remaining full scope

Correct JDK, Guava, Eclipse Collections, Spring Framework and Apache Commons Collections
API names and their M3 owners remain unchanged. Existing live filtered and sorted wrappers
are not replaced by masks. The read-only filtered-list inherited-iterator rescanning issue
was inspected but is not changed here: callback/exception timing needs its own compatibility
review. Full wrapper/API closure, compiled RWT/GWT/Nebula apps, toolkit painting, full custom
Maven reactor/OpenJDK/jtreg/platform/GC integration and comparative benchmarks remain open.
See the task evidence for executed versus authored gates; SDK tests are not inferred from
manifest-kernel tests. All changes in this packet remain in the Apache-2.0 M3 module.
