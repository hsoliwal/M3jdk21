<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# M3 five-provider mask/count selection

Canonical producer: Synexia Maven/OpenRewrite hash-pinned Java/text recipes.
Receiver: M3JDK21 stacked on #1174 commit dbe094bcc66655ed09f9d77529adc9f6015fda95.

Two ABSENT-before Java files extend the *existing* direct five-library runtime:
M3FiveProviderFlagSelection.java and M3FiveProviderFlagSelectionTest.java.
No other production class is touched. Existing M3FiveProviderDirectSdk,
M3SpringArrayMultiValueMap, old M3LibraryCollections factory, 33 factories
and 20 collectors are retained exactly.

M3ObjectCounts owns canonical references, indexed primitive count and 32-bit
flag arrays. The existing M3IndexPathMasks Java/C17 JNI selects primitive
physical offsets; no Java objects or callbacks enter native. A selected
M3ObjectCounts snapshot powers real Guava Multiset, Eclipse MutableBag and
Apache MultiSet with distinct contracts and shared selected mutations.
Independent JDK Map/NavigableMap and direct Spring MultiValueMap projections
use existing M3 arrays/links and preserve their own distinct-key semantics.
No provider type is impersonated and the captured data does not track later
source mutations. Comparator-colliding JDK sorted projection refuses.

Source Java postimage SHA256 values:
5becc5f604dc0309fe000c5388e76488f11f22e3f725ae01f073f2c3e85b73c0
511c42a8f8cf2119770931307ef4c29f35e0fdbbc1ba438e170aa151905ca76e

Parent five-provider Maven and checked JNI receipts apply to the historical
parent only. New exact-head Maven/OpenRewrite fixed point/refusal, full actual
provider JUnit and explicit required C17 JNI parity are PENDING. Draft/HOLD
until those gates pass. Full all-class library parity, JDK image/jtreg/JCK,
serialization, concurrent JMM and quantitative heap/CPU remain unqualified.
Original OpenJDK GPL/CPE, Eclipse EPL/EDL, Apache-2.0 and other donor
rights remain unchanged. LeetCode, HackerRank and GFG are problem taxonomies
only, with no copied challenge submissions or editorial source.
