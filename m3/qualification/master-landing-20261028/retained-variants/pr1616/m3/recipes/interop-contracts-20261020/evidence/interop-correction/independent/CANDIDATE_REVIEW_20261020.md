<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Independent interop candidate review, 2026-10-20

**No introduced defect identified in the 12 reviewed postimages.** The current snapshot includes the additional Apache Bag fixture. Review was read-only: no product, candidate, canonical, POM, or shared build outputs were changed. Exact preimage/postimage and consulted owner SHA-256 values are in `CANDIDATE_REVIEW_20261020.json`.

## Contract and gate assessment

- **Compiler repairs:** splitting the compound `var` declaration and adding the missing `Map` import preserve the same operations. The Spring factory call now references the existing public owner. The Apache method reference preserves the `Integer` nonnegative predicate and lets the existing facade provide its functional target type; no warning suppression or compiler-flag relaxation is introduced.
- **Inventory and factory identity:** the source TSV has 25 entries: JDK 6, Guava 7, Eclipse Collections 5, Spring 4, Apache 3. The BiMap/BidiMap factories already exist. Updating the fixed expectations and overload fixture to 25/26 preserves exact-count checks, descriptor validation, bad-input cases, family counts, and the production verifier's refusal of unmapped public factories. The Spring class assertion agrees with the current factory and binding; owned list and caller-alias tests remain.
- **Stable merge:** the existing array atom explicitly places all equal-key occurrences from the left window before equal-key occurrences from the right. Merging `[0,1,2]` with itself under keys `[a,a,b]` therefore yields `[0,1,0,1,2,2]`. The corrected fixture preserves distinct occurrence identity. Existing unsorted-input, unrelated-payload, physical-index, and 1700-trial checks remain.
- **Guava native selection:** the native-present branch actually requests `NATIVE_REQUIRED` and compares full 32-bit selection, including the sign bit and an excluded bit, against the independently constructed expected entries. Native absence still requires `UnsatisfiedLinkError`. Geometry refusal, detached-selection behavior, and the original 6400-step corpus remain. This change is consistent with the selector's explicit availability/refusal contract.
- **Spring mutation outcomes:** only `add`, `addIfAbsent`, and `addAll` use the new helper. Each owner executes independently; equal exception classes or two successes are required. `Error`/assertion failures are not swallowed. The original random seed, 4500 operations, other return comparisons, fixed-size/aliased list fixtures, and all six post-operation state checks remain. This agrees with the earlier strictly compiled and executed independent Spring probe and its 209 matching refusals.
- **Apache retention:** the independent occurrence-budget list implements the existing minimum-cardinality contract without calling either counted implementation's retention atom. The original 6000-operation stream, return/state/count/equality/hash checks remain. Donor equality is also asserted on the exact common domain; after divergence the next operation starts from the contract oracle. The added oversized-input witness asserts both the unchanged M3 result and the pinned HashBag 4.5.0 quirk. No production behavior changes.

## Qualification limits and existing follow-up

This review did not run Maven. The owning fresh builds, canonical recipe checks, receiver checks, and separate Java/native evidence still determine qualification. `M3LibraryBindingsSelfTest` is a standalone main with no JUnit caller, so an explicit existing-main invocation is needed to claim its execution. Native branch coverage likewise needs evidence of both availability modes.

An existing metadata mismatch remains outside the changed lines: both `SDK_FAMILIES.md` and `sdk-family-bindings.tsv` list `M3LinkedList` for `guavaListMultimap` values while the current `M3LibraryCollections` factory uses `M3ArrayList::new`. The count corrections are valid; they do not establish storage-owner metadata parity. This was reported to the parent as a separate existing follow-up.

Candidate-set SHA-256 (sorted postimage SHA-256, two spaces, relative path, newline): `d2ca682f4a4a66c0bd2f6d0ddd2b456c3ab8038f4c8985caf3af59b4452dc8a8`.
