<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Composed String and Java/JNI collection integration

This is an M3JDK21 receiving checkpoint. Runtime behavior lives in the existing
JDK and its owned modules. Reusable transformation recipes, source custody and
donor comparison live canonically in Synexia.

The central String benefit is composition: an ordinary String can retain a
canonical native/mapped owner and immutable range coordinates, while concat and
slice operations share those owners. This work preserves that representation and
improves traversal, exact tuple reuse, exceptional ownership cleanup and native
write bounds. JNI compatibility projections retain their explicit lifetimes.

Implementation review: [M3JDK21 #1657](https://github.com/hsoliwal/M3jdk21/pull/1657).
Canonical recipes: [Synexia #12680](https://github.com/hsoliwal/com.synexia/pull/12680).
The exact commits, file hashes and receiving epochs are in `SOURCE_RECEIPT.json`.

## Changes and retained contracts

| Area | Refined behavioral atoms | Contract retained or repaired |
| --- | --- | --- |
| Composed String traversal | Existing code-unit cursor for long comparisons/prefixes | UTF-16 code-unit order, first-unit shortcut, null/bounds order, native I/O bridge and search guards |
| Native String ownership | Six atom factories; weak cleanup-record publication; tuple retention | Exactly one owner for unpublished cleanup, original exception, correct native/tuple accounting, same child and payload owners |
| Code-point conversion | Pre-write coder/capacity checks | Valid UTF-16 output and descriptors; reject insufficient native capacity before writing, including caller-array mutation between passes |
| Compiler concat | Existing nullable reference stringifier recovered from prior qualified work | Typed null and null-returning toString become the required text in admitted and flat-fallback paths; conversion order preserved |
| Tuple lookup | Existing owner-and-coordinate identity proof before the leaf walker | Exact ordered-child reuse; different ranges, parenthesizations and route collisions still use exact comparison |
| Filtered/sorted navigation | Cursor creation outside range-construction exception recovery | Successful comparator/predicate/iterator order; original cursor exception reaches caller |
| Deque removal | Query-directed equality | First/last visible occurrence, hidden values, nulls, reverse views and read-only refusal |
| Indirect radix | Java/JNI uniform-digit skip and odd-active-pass publication | Stable unsigned key/row association, masks/slices, original scratch owners and native ABI |
| Selection and callbacks | Packed bit scratch, saved reentrant state, capture generation | One predicate call per occurrence; private output indexes; callback invalidation survives capture |
| Public identity views | Known-owner classification in java.util | Reference membership and entry hashing, while original wrappers retain traversal/mutation responsibility |
| JNI empty selection | Skip payload reads for zero selected rows | Bounds, state rescans, drift exceptions and output ownership remain |

The public interfaces are unchanged. Repairs of preexisting failures are declared
as repairs, with baseline witnesses. Current binary-carry predicate composition,
copy-on-write bulk mutation and private selected-directory reuse were retained
when an older donor whole file would have replaced them.

## Search and reuse the catalogue

The receiving catalogue is
`m3/collections/algorithm-catalog/composed-integration-20261021.tsv`. It is a
UTF-8 TSV organized by problem category, with incumbent, candidate, exact source,
decision, contract, complexity/storage, performance scope, rights and validation.
It contains 43 decisions, including accepted refinements, rejected alternatives
and unresolved cases. The adjacent catalogue notice explains source rights.

For example, from the repository root:

```sh
rg -n 'COMPOSITE_TUPLE|NATIVE_OWNERSHIP|PUBLIC_IDENTITY|RADIX' \
  m3/collections/algorithm-catalog/composed-integration-20261021.tsv
rg -n 'PACKED_OCCURRENCE_SELECTION|SEGMENTED_TRAVERSAL' \
  m3/collections/algorithm-catalog
```

GitHub donors and prior Synexia qualifications supply reusable implementations.
LeetCode, HackerRank and GeeksforGeeks supply additional problem categories and
review cases. No challenge submission, editorial implementation or tutorial
source body was copied by this integration. Existing Apache, OpenJDK and other
per-file notices are preserved. `M3_INVARIANT.md` specifies the maintained
discovery/refinement/verification cycle.

## Evidence and its scope

`evidence/collections` records the full owning Java/JNI suite, strict native
checks, generated ABI, JMOD packaging, installed class origins and finite resource
measurements. `evidence/core` records the actual source-built JDK and per-action
jtreg outcomes, including original failures. `FILE_PASSES.tsv` and the canonical
source receipts distinguish SDK transformation proof from runtime behavior.

The original implementation epoch built a full Linux x86-64 server JDK with
warnings-as-errors, and generated the default CDS archive variants with M3
storage disabled. The linked image contained all 512 collection production
classes and ten JNI libraries; 491 tests ran with JNI required and no failures,
errors or skips. Production class-load observations came from the image modules,
with no duplicate production classes supplied by test class paths.

The same 28 selected core tests ran against both built JDK epochs:

| Built JDK epoch | Passed | Failed | Timeouts | Main actions |
| --- | ---: | ---: | ---: | ---: |
| E1 original implementation | 23 | 4 | 1 | 63 |
| E2 with qualified receiving successors | 26 | 2 | 0 | 67 |

All 23 E1 passing tests remain passing. E2 repairs nullable concat, corrects the
native-budget fixture shape and reuses exact child coordinates for large logical
compositions. The expanded canonical fixture passes 59 checks in each compact
mode; the unchanged overflow fixture passes 111 in each mode; the concat DAG
passes 23; native-budget metadata passes 6,660 in each of three modes.

The two inherited failures remain: the restored char-array test requests absent
private methods, and PoolBounds assumes an unchanged global native counter
across incidental String work and GC. No assertions were weakened to hide them.
The E2 relink independently verifies the actual installed String owners, public
collection behavior and JNI kernels. The 491-test full collection run belongs to
E1; its 567 sealed collection inputs are unchanged in E2. Read `OPEN_GATES.md`
before interpreting these finite results as broader platform acceptance.

The resource comparison deliberately keeps mixed results. The paired radix
measurements used Temurin 21 with staged baseline/candidate classes, separately
from the built and linked SDK checks. At 65,536 rows, these limited runs improved repeated/narrow keys and included a native
varying-key CPU regression. Scratch ownership and retained-index accounting were
unchanged. Packed selection reduced the measured one-million-row zero-survivor
temporary allocation; the actual raw values and environment scope accompany the
receipt. These observations are not a general throughput or whole-heap claim.

## Reproduce and continue

Start from the recorded receiving base and the exact canonical source commit.
Use the canonical host's documented dataset preparation, actual SDK tests and
receiver commands. Apply qualified stages serially. For final replay, use the
published original-to-final aggregate appropriate to that epoch; an earlier
recipe correctly refuses a later successor's changed output.

Use the existing owning collection Maven/native gate and JMOD packager described
in the evidence packet. Build the JDK with a Java 21 boot JDK and required native
development headers; this run used the standard release/server/headless build,
bundled FreeType and the supported direct javac mode. Test with the built image
and the actual JNI bridges. Environment-specific temporary tool paths are not
runtime dependencies or product configuration.

Append new evidence, retain old source seals, and keep every new algorithm choice
in the catalogue. Do not advance a wider String/Arrays/Collections/AST phase from
this finite checkpoint alone.
