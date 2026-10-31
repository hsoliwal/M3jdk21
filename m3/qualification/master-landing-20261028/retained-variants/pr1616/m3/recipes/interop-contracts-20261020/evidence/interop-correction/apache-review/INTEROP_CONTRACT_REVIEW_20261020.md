<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Interop failure contract review — 2026-10-20

## Decision

The two observed Apache Bag and Spring multivalue failures do **not** justify changing their production owners. Preserve the current M3 contracts and correct the test oracles through exact-source canonical recipes. The Apache failure exposes an actual incompatibility with one pinned HashBag implementation branch; the Spring failure exposes a missing exception-outcome comparison in the fixture. These are different findings and must keep separate catalogue dispositions.

This review covers sources received from M3JDK21 parent `2779aba3f98f31c324be43f731be7914e1a05935`. No product, canonical recipe, or correction-agent candidate source was changed by this reviewer. Only this review directory was written. Earlier final/upstream/sibling reports remain unchanged.

| Observed failure | Root cause | Contract-preserving disposition |
| --- | --- | --- |
| `M3ApacheBagContractTest.randomizedOfficialHashBagOracle`, first mismatch at step 702, donor size 0 versus M3 size 2 | The test treats Commons HashBag 4.5.0 as an unconditional oracle, although its overrepresented-input retain branch differs from the M3 method's explicit minimum-count contract and the Bag API description | Keep M3 source unchanged. Retain the full seed and state assertions against an independent Bag-contract occurrence oracle. Add explicit evidence/assertions for the pinned donor divergence and limit compatibility claims accordingly |
| `M3SpringArrayContractTest.realSpringSixTwoDifferentialSeeded`, `UnsupportedOperationException` | A fixed-size caller list was installed earlier in the sequence. Both implementations correctly reject appending to that list. The fixture calls M3 first and exits before invoking the donor or comparing outcomes | Keep M3 source unchanged. Execute both operations independently, compare exception class or return value, then compare complete state and views after both success and failure |

The isolated module's `attempt-02-compile-fixes.log` recorded 158 tests, 6 failures, 1 error, 0 skipped. That is historical red evidence, not a final acceptance receipt. Other compile, stable-order, native-mode, factory-type, and factory-count corrections are owned by `/root/correction_memory`. The root's 29-target qualification and supplemental interop gates are distinct source closures.

## Apache: declared contract versus concrete donor behavior

The current `M3ApacheBag.retainAll` method explicitly promises `min(ownedCount, inputCount)`. Its array/count owner already implements that behavior. Apache's pinned Bag interface describes deleting excess owned copies when the bag contains more than the input and removing absent keys. The M3 class also explicitly excludes concrete HashBag type identity. There is no basis to silently replace the declared M3 rule with a different concrete implementation's behavior.

Pinned Commons release commit: `7f7fb0244abc940a2e9dd28b67508c0483a58c3e` (`rel/commons-collections-4.5.0`). Its `AbstractMapBag.retainAll(Bag<?>)` retains the incoming count only when that count is positive and no greater than the owned count; otherwise it removes every owned copy. The `Collection<?>` overload funnels both Bag and ordinary collection inputs into that implementation. See the exact [Bag contract source](https://github.com/apache/commons-collections/blob/7f7fb0244abc940a2e9dd28b67508c0483a58c3e/src/main/java/org/apache/commons/collections4/Bag.java) and [AbstractMapBag implementation source](https://github.com/apache/commons-collections/blob/7f7fb0244abc940a2e9dd28b67508c0483a58c3e/src/main/java/org/apache/commons/collections4/bag/AbstractMapBag.java).

Executed minimal results for one key:

| Owned copies | Input copies | M3 retained | HashBag retained | M3 changed | HashBag changed |
| ---: | ---: | ---: | ---: | --- | --- |
| 2 | 1 | 1 | 1 | true | true |
| 2 | 2 | 2 | 2 | false | false |
| 2 | 3 | 2 | 0 | false | true |

An independent probe compiled the exact current M3 source with Java 21, `--release 21 -proc:none -Xlint:all -Werror`, against the root-installed M3 collections JAR and the real Commons 4.5.0 JAR. It then ran with assertions enabled. `APACHE_PROBE_RECEIPT.json` records commands, source/JAR seals, exit statuses, and log hashes.

Results executed by this reviewer:

- **294 retention cases passed**: owned/input counts 0 through 6, null and non-null keys, and three input families (`ArrayList`, `HashBag`, `M3ApacheBag`). Exactly **90 cases explicitly demonstrated the donor divergence**. Both implementations' counts, total size, and modification result were checked against their respective rules.
- **The original failure reproduced at step 702** using unchanged seed `20261018L` and original random consumption. Input was `[6, 6, 6]`; the bag held two copies of key 6. HashBag ended empty; M3 retained those two copies.
- **All 6,000 original-seed operations passed against the independent Bag-contract oracle**, with **51,079 assertions**. Operation counts were `[983, 986, 994, 1026, 1027, 984]` for counted add, counted remove, full-key remove, add-all, remove-all, retain-all. The reference stores individual occurrences in a JDK list and consumes an independent input occurrence budget during retain. It does not use M3's counted-directory algorithm. Return values, size, unique keys, every count, symmetric Bag equality, and hash codes are checked. Real HashBag values are reconstructed from the reference occurrences for equality/hash comparisons, without invoking the incompatible donor retain branch.

### Required fixture and catalogue correction

Do not skip the overrepresented-input cases, shorten the seed, suppress failure, weaken size/count/equality/hash assertions, or change production behavior to make the old oracle green. Use the independent occurrence model for the documented Bag contract and explicitly characterize the concrete donor branch. Retain real HashBag differential checks where the contracts coincide. Rename or clarify the existing test description so it no longer claims unconditional HashBag-oracle equivalence.

Record the algorithm category as cardinality-aware multiset intersection. The selected existing M3 algorithm is the minimum-count rule. The rejected replacement is the pinned HashBag overrepresentation branch, because it changes the declared M3 contract. This is a contract rejection, **not** a performance rejection. No CPU, allocation, retained-heap, native-memory, serialization, or full Commons API claim follows from these finite checks. The incompatibility must remain visible to callers requiring exact HashBag concrete behavior.

## Spring: legitimate exception and post-failure state

`M3SpringArrayMultiValueMap` explicitly preserves caller list identity and mutability for `put`/`putAll`. Its `addAll(key, values)` appends to the stored list. Spring 6.2.0's real `MultiValueMapAdapter` does the same; the donor's `put` stores the caller list reference. Therefore a fixed-size list installed through `putIfAbsent`/`putAll` remains fixed-size, and a later growth operation legitimately throws `UnsupportedOperationException`. This behavior is required to retain the documented alias and mutability contract. See the pinned [Spring adapter source](https://github.com/spring-projects/spring-framework/blob/5024bb72279188f1d0dcfe19e8abdd4bdb9887c8/spring-core/src/main/java/org/springframework/util/MultiValueMapAdapter.java) from `v6.2.0`.

The original fixture performs the M3 append before the donor append in case 4. The first legitimate M3 exception aborts the test before the donor operation, so that stack trace cannot establish a behavioral mismatch.

The separate `/root/correction_memory/interop_semantics` agent executed a strict-compile probe of the exact current Spring owner. This reviewer independently read that probe, logs, current owner, original fixture, and pinned official donor source; the probe was **not rerun** by this reviewer. Its source copy SHA-256 equals the current product source SHA-256 (`8062247a92ee9dc90d313b715ba9158e47c53b3e7848508a5fdd54ddaaaecb96`).

Reviewed peer execution evidence:

- Minimal borrowed `Arrays.asList` witness: matching `UnsupportedOperationException`, preserved caller identity, and equal post-failure map state.
- Original seed `0x20261017L`, **4,500 operations**, **209 matching runtime exceptions**, all seven per-operation outcome/state comparisons passed.
- The first seeded refusal was step 23, operation 4, key 30, value 3, with the existing list `[19]`. Both maps retained the same state afterward.

The corrected fixture should compare a structured outcome (returned value or precise runtime-exception class), execute both sides, and retain every existing map/size/first-value/copy/key-order/live-view check after each operation. Catch only the expected operation's runtime failure; do not swallow assertion failures, setup failures, or differences between the two outcomes. The fixed-size and read-only lists must remain in the input corpus.

This finding is scoped to the reported seeded failure and valid mapped-list operations. It does not establish every Spring method, invalid-input side effect, concurrent behavior, serialized form, or installed runtime integration.

## Evidence and ownership

`DONOR_SOURCE_PINS.json` records release and commit identities, Git blob identities, license, and use. Official donor source was inspected, not copied into runtime or recipe payloads. The checked M3 source copy in this directory retains its original first-party Apache-2.0 notice. The independent probe is first-party Apache-2.0 review evidence.

`REVIEW_SOURCE_SEALS.json` seals the current two production owners, original fixtures, peer Spring probe/logs, and triggering red log. `APACHE_PROBE_RECEIPT.json`, `apache-compile.log`, and `apache-contract-probe.log` bind this reviewer's actual execution. The Spring peer logs remain in `work-interop-fix-20261020/independent/`; their hashes here distinguish reviewed peer evidence from this reviewer's execution.

Any fixture correction must be authored canonically in Synexia, preserve exact before/after seals and immutable failed-attempt memory, then receive into M3JDK21 through the qualified recipe. These probe passes alone do not accept the correction: actual whole interop clean verification in both required modes and canonical recipe fixed-point/refusal checks remain with the implementation owner/root. No receiving phase advances from this review.
