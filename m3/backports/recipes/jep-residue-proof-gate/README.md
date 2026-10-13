# JEP residue queue proof-gate repair — 2026-10-13

This is a test/evidence correctness repair, **not an implementation claim for any JEP**.

## Baseline fault

`test_detects_packet_and_recipe_class_evidence` creates only JEPs 458 and 485 but previously
asserted evidence for JEPs 423/484/523. The isolated test always raised `KeyError(423)` before
validating its intended materialized-receipt and recipe-class behavior.

The repaired suite separates the packet-only evidence cases 423/484/523 into their own
explicit fixture. It also tests false-positive packet prefixes such as `jep-4230-foo`
and refuses duplicate/mismatched priority metadata.

## Portable recipe custody

Canonical Apache-2.0 owner: hsoliwal/com.synexia PR #10724, at
`05c969ec478e0b2a3e2d03e46aef48e0f60df060`.

The source-sealed Synexia recipe is
`M3Jdk21JepResidueProofRepairRecipe`, applied to only:

- `m3/backports/jep_residue.py`
- `m3/backports/test_jep_residue.py`

This JDK receiver uses the existing `M3Jdk21HashPinnedTextSnapshotRecipe`; no new
general recipe framework is introduced. The mirrored manifest requires exact SHA-256
pre/post images and fails closed on stale input. JUnit must prove a zero-change second
pass, and the Python `unittest` suite must execute for behavioral acceptance.

## Proof boundaries

The JEP released denominator remains 85. This repair does not alter the 42-item
pending/hold residue denominator, candidate dispositions, dependency closure, source
diffs, OpenJDK product code, HotSpot, JNI, or promotion state.

No completion claim may be made until the build/JUnit and Python tests execute.
