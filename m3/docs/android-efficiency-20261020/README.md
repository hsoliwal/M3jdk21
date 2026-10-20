<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 current-owner efficiency and common JNI receiver

M3JDK21 owns these runtime implementations. This receiver pins the reusable
Synexia recipes at commit `f29989d9d1ee1097ad4a50dda277dabfe1496da4`, [canonical PR #12564](https://github.com/hsoliwal/com.synexia/pull/12564).
It extends the existing collections/Swing receiving stack at
`8e868958f017482cfd6c5085869263c92a011cf6` ([#1525](https://github.com/hsoliwal/M3JDK21/pull/1525),
based on [#1522](https://github.com/hsoliwal/M3JDK21/pull/1522)).
Existing wrapper behavior, namespace ownership and default-off Swing remain.

## Three receiving atoms

| Existing owner | Change | Preserved boundary |
| --- | --- | --- |
| `jdk.internal.mindex.M3MappedStringBacking.Pages` | Read scalar metadata directly from existing mapped pages | Byte order, cursors, range/page errors, mapped lifetime, wire format, public views and API |
| `m3_state_relation_compose` in current `libjava` source | Receive the existing canonical nonzero set-bit traversal and full-mask union atom | Original portable body, validation before writes, aliasing, dimensions/bit63, current JNI/error/repeat tail and four exports |
| `m3/arrays/src/main/native/CMakeLists.txt` | Use actual NDK target JNI headers and 16 KiB Android link settings | Every non-Android discovery path, existing Java/native owners, ABI, C11 minimum, warnings, tests, LICENSE/NOTICE and fallback |

The runtime source files are exact outputs from the successful OpenRewrite 8.17.1
SDK materialization, not independent hand edits. The runtime recipe guards nine
current files for two changes; the build recipe guards nine for one change.
Fifteen protected receiving files are unchanged. Source guards bind both current
master `d6d76b563050fc13c4fa79bc494edf29f1614b37` and the selected stack where applicable.

## Common JNI invariant

The [common JNI receiving requirement](COMMON_JNI.md) applies across Android,
Apple phone/tablet targets and other native platforms. Shared Java/native
contracts belong to the existing M3JDK21 owners and shared native source; target
adapters supply platform/VM facilities. They must preserve values, offsets,
masks, ownership, exceptions, thread attachment and native resource lifetimes.

The current shared `libjava` authority and existing arrays JNI library are reused.
The arrays change receives the already existing canonical Android build
mechanism. Apple target VM/JNI integration and end-to-end phone runtime support
remain unimplemented or unqualified; the build recipe does not claim otherwise.

## Executed host evidence

- **Recipe:** eleven actual SDK tests, zero failures/errors/skips; exact outputs,
  fixed points, refusal and serial resumption. Sixteen bounded runtime schedules.
  Java LST and full canonical reactor execution are not claimed.
- **Mapped metadata:** unchanged existing test as a standalone main, Java 21 fatal
  compilation, six fresh JVMs per source and 7,291 checks per trial. API and
  observation hashes match. On the frozen 1,024-alias fixture, median allocation
  per opening was **3,802,392 → 65,688 bytes (98.27% lower)**. This is admission
  work on this host; already cached steady-state queries are not the measured path.
- **Native relation:** baseline/candidate/portable/UBSan/portable UBSan pass the
  existing full JNI proof (71,748 cases / 149,658 checks per native variant),
  65,671 C assertions, 524,805 sparse C checks and 441,671 sparse JNI checks.
  Overlapping counts are not added as unique cases. API, headers and exports match.
- **Timing:** the first unpinned zero-control series held at +12.26%. Its
  predeclared CPU-0 investigation passed with the same source (83.346 versus
  92.079 ns/op). Both series and all 126 samples remain. No universal speed claim.
- **Common build:** baseline and candidate each pass 187,008 native differential
  cases, five tests with the actual loaded library under `-Xcheck:jni`, and
  three selected fallback tests in a fresh JVM. The complete host libraries are
  byte-identical; all eight protected inputs and normalized compile commands match.

The complete reports and raw proof custody remain canonical:

- [Task design, source guards and reproduction](https://github.com/hsoliwal/com.synexia/tree/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/README.md)
- [Java qualification](https://github.com/hsoliwal/com.synexia/tree/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/java-leaf/QUALIFICATION.md)
- [Native qualification and both timing series](https://github.com/hsoliwal/com.synexia/tree/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/native-leaf/QUALIFICATION.md)
- [Common JNI build qualification](https://github.com/hsoliwal/com.synexia/tree/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/common-jni-build/QUALIFICATION.md)
- [Actual SDK materialization](https://github.com/hsoliwal/com.synexia/tree/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/execution/SDK_MATERIALIZATION_STAGE_2.json)
- [Donor/category evaluation](https://github.com/hsoliwal/com.synexia/tree/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-openrewrite-recipes/recipes/m3-android-efficiency-20261020/DONOR_EVALUATION.md)
- [Existing catalogue, extended to twenty-one rows](https://github.com/hsoliwal/com.synexia/blob/f29989d9d1ee1097ad4a50dda277dabfe1496da4/synexia-indexstring/docs/search-algorithm-catalog.tsv)

The original six-row catalogue prefix, rejected candidates, failed compiler
invocations and current measurements are retained. GitHub/AOSP implementations
and LeetCode, HackerRank and GeeksforGeeks categories informed selection; no
challenge submission, editorial or article implementation was copied.

## Contract-preserving refinement and lineage

Synexia M3 maintains reusable validated transformation recipes, admits exact
per-file contracts, and improves algorithm choices through accumulated category
comparison. Independent behavioral atoms are replaced only when their composed
contract is preserved. No new transformation engine or runtime facade is added.

`../name-mapping.json` retains the existing naming meanings and migration
identities, adds three scoped refinement records, and records exact canonical
and receiving commits. Existing active pins that would otherwise be stale are
reconciled explicitly; complete prior records and target pins remain in
`history/`. The current validator is run against actual receiving bytes.
Product acceptance and migration completion remain open.

## Remaining acceptance gates

The receiving phase remains **STRING / QUALIFICATION_REQUIRED**.
Source-built integrated M3JDK21/HotSpot and full libjava, installed images,
jtreg/JCK, Android NDK/ELF/package/device checks, Apple VM/JNI/signing/device
execution, and SWT/Swing/Skija surfaces, lifecycle, IME and accessibility remain
unexecuted here. No usable Android device/NDK or Apple SDK/VM runner was admitted.
This PR is for review; it does not declare all-phone support complete.
