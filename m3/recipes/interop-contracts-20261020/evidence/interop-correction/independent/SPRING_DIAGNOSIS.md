<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Spring interop differential: legitimate exceptions must be compared

## Finding

The retained interop failure in `M3SpringArrayContractTest.realSpringSixTwoDifferentialSeeded` is a test-oracle defect. The test aborts when the M3 side throws a legitimate `UnsupportedOperationException` before executing the corresponding Spring operation. The current production owner preserves the existing Spring contract: lists supplied through `put`/`putAll` keep their reference and mutability; `asSingleValueMap().put` can install an immutable singleton list. Attempting to append to one of those lists can therefore fail on both sides.

The original failing call is the seeded loop's `case 4`, `actual.addAll(key, input)`, at line 90 in the unmodified test. Both implementations are validly operating on a previously stored fixed-size list. This is established by the actual pinned donor artifact and executed differential below, rather than inferring behavior from a newer Spring release.

## Independent evidence

An exact copy of current `M3SpringArrayMultiValueMap.java` was compiled alongside `SpringExceptionParityProbe.java` with Java 21, `-Xlint:all -Werror`. Execution used `-ea -Xcheck:jni` and the tested owning collection jar, with the actual `org.springframework:spring-core:6.2.0` artifact.

The minimal fixture stores a caller-owned `Arrays.asList(1)` at key 2 and appends `[3, 4]`. Both M3 and Spring throw exactly `java.lang.UnsupportedOperationException`, retain the same list reference, and preserve map content.

The probe then preserves the original seed `0x20261017L`, 4,500 operations and 13-way operation selection. It executes the donor and M3 operations independently, compares their return values or runtime-exception classes, and retains the original map/size/first-value/copy/key-order/live-view observations after either result.

```text
SPRING_MINIMAL_BORROWED_FIXED_LIST_PASS exception=java.lang.UnsupportedOperationException
SPRING_SEEDED_EXCEPTION_PARITY_PASS operations=4500 refusals=209 checksPerOperation=7
```

The first matching seeded refusal occurs at **step 23**, operation 4 (`addAll`), key **30**, whose current list is `[19]`. Both sides throw and the state is unchanged. All 209 refusal outcomes and all remaining successful operations match; no production difference was observed in this corpus.

Exact pins and commands are in [SPRING_RECEIPT.json](SPRING_RECEIPT.json), [spring-0.log](spring-0.log) and [spring-1.log](spring-1.log). The current source SHA-256 is `8062247a92ee9dc90d313b715ba9158e47c53b3e7848508a5fdd54ddaaaecb96`; the pinned Spring jar SHA-256 is `3b82629da38717dd616a70ec1055f9a8e208d58132071a6314a32159f8bce162`. [Spring-MultiValueMapAdapter-javap.log](Spring-MultiValueMapAdapter-javap.log) retains bytecode inspection of that exact artifact. The retained Spring-derived inspection remains subject to the donor's Apache-2.0 license and original authorship; it is diagnostic evidence, not replacement source.

## Proposed correction

Keep the production owner unchanged. Repair the differential to execute both sides of operations that can mutate stored lists, including legitimate refusal outcomes. A minimal change wraps the void mutation branches 0 (`add`), 2 (`addIfAbsent`) and 4 (`addAll`) in a helper that independently executes each `Runnable`, captures `RuntimeException.getClass()` or null on success, and compares the classes. Retain every existing return-value and state assertion and the original random sequence. Do not catch assertion errors, remove immutable-list inputs, broaden list mutability, or replace borrowed lists with mutable copies.

The independent probe compares outcomes for all 13 operations. Its source is retained for review; it is not a claim that the proposed owning JUnit test correction has already been applied or qualified. The parent task owns that isolated change and subsequent full `clean verify`.

## Scope

No product source, canonical recipe input, shared module source or shared build output was modified by this diagnosis. Only this independent directory contains copies, probe source, compiled probe outputs and evidence. The result qualifies this finite original-seed corpus and minimal refusal fixture; it does not establish every Spring method or platform. The Apache Bag finding was handed to the independent `final_review` agent because it involves a separate conflict between documented minimum-cardinality behavior and the pinned donor implementation.
