<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Packed-token continuation contract

M3JDK21 is the receiving Java/JNI product. This pass builds on collection PR 1357
at c71bc6e93bcec6c470665cba487ab64e507ebdd9 and consumes token PR 1338 at
3e1a0b7b7d2498a62550861a6a5a9bd88a2314d7, whose canonical source is Synexia
PR 12346 at 710588f0ca8ad1c2b0d6d9536f16aa881078b647. No collection source changes
in this continuation. Three previously recorded collection standalone failures
remain open. String remains the active global receiving phase.

## Per-file sequence and frozen behavior

1. Receive the three exact donor Java/native/test blobs through the existing
   class-backed hash-pinned Java/text OpenRewrite recipes. The donor's Python
   replay is evidence, not a replacement for actual SDK qualification.
2. Refine only the private allowed-mask construction in M3RX7.java: accumulate
   each 64-token word in a local long, then store once. Preserve every public
   descriptor, token transition, accepting flag, validation order, resource
   budget and Java/native fallback decision of the token donor.
3. Add independent boundary/differential/native probes. Preserve the original
   donor test unchanged. Qualification sources use the same exact native kernel;
   host RegisterNatives plumbing is test-only and cannot prove libjava integration.

TokenIndex is character-DFA composition over nonempty ASCII vocabulary slices.
Its masks mean reachability through the underlying ASCII DFA, not reachability
restricted to the supplied vocabulary. Dead state -1 is absorbing, but invalid
token IDs must still fail. Unused high bits in the last mask word stay zero.
Coordinates may overlap, repeat and occur out of order. Input payload is not
retained after construction. The caller must not concurrently mutate construction
inputs. The Java CharSequence path must not flatten, and must still observe a
non-ASCII unit after the DFA becomes dead. Chunk geometry is validated before
the text budget decision. Pattern remains authoritative for verified matching.

The historical M3RX7 String descriptors remain callable. The donor adds internal
CharSequence/chunk/token entry points; this is additive internal capability,
not a change to exported java.lang or java.util APIs. The mask refinement must
have exactly the donor's public/protected surface and javac-generated JNI header.

## Independent evidence and limits

Compare every generated row against direct scalar DFA execution, and masks
against independently computed transitive reachability. Cover 0/1/63/64/65/127/
128/129 token boundaries, signed bit 63, empty streams, overlapping coordinates,
unproductive states, invalid indices, budget refusal and post-construction
mutation. Native validation must reject malformed dimensions/targets/offsets/
bytes before writing output. Require exact Java/native rows, -Xcheck:jni and
fatal undefined-behavior sanitizer execution. Missing symbols must use Java.

Record CPU, allocation, retained primitive bytes and process-memory observations
for identical workloads without promising throughput improvement. Compare old
and new masks independently; do not use the optimized implementation as its own
oracle. Run patch -> lint -> compile -> tests -> runtime and exact recipe replay.

This does not implement T-Rex predicate/capture syntax, a Java lexer, a scope-aware
rename, or a new tokenizer. Existing IndexRegex/MIndexTokenPattern/MIndexASTQuery
and M3AST.Unit.project retain those distinct future ownership decisions. Whole
JDK build, jtreg, HotSpot/GC/CDS and non-Linux acceptance require separate receipts.
