# Indexed-tree insertion validation

Ordinary nonempty `M3TreeMap.put` used to compare the key with itself before
descending through existing keys. The descent now validates the key directly.
For reverse-order insertions of 3 then 2, the comparison sequence changes from
`3:3, 2:2, 2:3` to `3:3, 2:3`. The first insertion keeps its admission check.
Bounded, reversed, lazy-value and key-only modes retain their existing checks.
Payload arrays, integer AVL links, storage layout and public interfaces remain
unchanged. The production diff adds six lines and removes one.

The donor is the existing Apache-2.0 M3 implementation at PR1351. OpenJDK21
serves as a behavior reference; no OpenJDK implementation text was imported.
The per-file recipe records exact hashes and passed application, fixed-point,
inverse and unknown-preimage refusal checks. No challenge solution was copied.

Executed SDK checks: selected-class Java21 compilation with
`-Xlint:all,-module -Werror`; 88 unchanged public API rows; 132 frozen entry
cases; 113 frozen sorted-bulk cases; 5040 permutations and 98304 random
operations, with 1438195 checks and 238593 invariant calls. Those prewritten
fixtures were reused unchanged by a source-aware orchestrator.

The independent source-blind insertion checker retains its strict status
`DIFFERENCES_FROZEN_36`: 29 of 30 complete root records match JDK21. The only
remaining root difference is the existing empty-null exception message; both
baseline and candidate return a null message, while the JDK supplies text.
All six bounded operation bodies preserve the baseline. Full control records
retain the comparator changes from constructing their ordinary root owners;
none of these observations or messages were normalized away.

A separate independent checker matches all 58 frozen current Eclipse-owner
records exactly; its full candidate output is byte-identical to the original.
TRACE header/footer fields are static calibration metadata; actual candidate
execution and provider origins are bound separately in the checker RESULT.
This is supplemental evidence for a private current-owner candidate against
received11.1 dependencies. Current EC14 type closure, its Maven reactor,
JPMS/OSGi integration and complete Eclipse ownership remain open. The private
owner/helper are outside this PR's production scope.

Source-aware Codex review completed with no findings. Its structured report,
command and complete input manifest are retained. The earlier storage-stopped
attempt is preserved separately and is not counted as a successful review.

The standalone `oracle/JdkTreePutValidationOracle.java` requires Java21 and
uses only platform APIs at compile time. Run it with provider class argument
`java.util.TreeMap` for the reference, or `com.m3.collections.M3TreeMap` with
the qualified core classes on the classpath. Supply `-DfixtureOrigin` with the
fixture output directory and `-DexpectedJavaHome` with the JDK home. Candidate
runs additionally require `-DproviderOrigin` with the core class directory.
The source, frozen original inputs, raw outputs and provider bindings are
included; no fixture changes were made after observing the candidate.

This one-file correction is stacked on PR1351. It is not a full clean-build,
five-library completeness, JNI CRUD, allocation-reduction or measured
throughput claim. The broader M3 collection work continues independently.
