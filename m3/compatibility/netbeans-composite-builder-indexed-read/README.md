<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->

# Composite builder indexed-read receiving proof

**Exact-source Java qualification passed; the integrated M3JDK21 image is blocked.**
This packet records the bounded String refinement applied to target baseline
`677437be295b23f7ccf7409be287008944d0e8b0`. It does not advance the receiving phase
or declare any family complete.

## Applied source and custody

The Synexia-owned `com.synexia.m3.CompositeBuilderIndexedRead` recipe emitted four
sealed outputs: `M3StringBuilderLane.java`, `AbstractStringBuilder.java`, the new
`M3CompositeBuilderIndexedReadTest.java`, and `m3/docs/name-mapping.json`. Their
preimages, postimages and actual receiving-tree hashes are in [receipt.json](receipt.json).
The existing builder owner gains a primitive cumulative piece-end index; public
`charAt`, `codePointAt` and `codePointBefore` reads retain pieces and flat backing.
Public names, UTF-16 semantics, synchronized StringBuffer entrypoints and payload
ownership remain with M3JDK21.

The [canonical qualification](https://github.com/hsoliwal/com.synexia/blob/e8702ffa7591ffb89027a97d44a09588d6276dd9/synexia-openrewrite-recipes/crates/m3-string-consumers/composite-builder/indexed-read/QUALIFICATION.md)
contains exact commands and evidence; the [canonical recipe PR](https://github.com/hsoliwal/com.synexia/pull/12496)
provides review history. The receiving receipt binds the SDK run, final Java
qualification, evidence bundle, measurements and closed build attempts by SHA-256.
NetBeans supplied occurrence-coordinate reference evidence; no NetBeans source body
or runtime dependency was imported. Existing OpenJDK and third-party rights remain intact.

## Executed Java gates

The actual named Maven/OpenRewrite gate passed 11 tests with zero failures, errors
or skips. Baseline, intermediate Java stage and final candidate each compiled
28 exact sources into 77 classes with Java 21, full selected lint and `-Werror`.
Public/protected ABI stayed unchanged across stages. The immutable original oracle
passed 40,000 scripts and 1,310,550 checks per successful run, including final
interpreter, C1 and C2 executions. Its earlier 600-second interpreter timeout remains
incomplete; the unchanged retry passed in 504.228 seconds under a separate 1,800-second bound.

Real jtreg passed 13 selected IDs. The new `#flagOff` ID completed six actions:
default, interpreter, C1, C2, disabled Compact Strings and Serial GC. Each executed
the same 192-script corpus, with 643,372 reads and 3,518,447 checks. Identical final
fixture bytes fail on baseline lane detachment and pass on the candidate, including
seams, bounds, growth, edits and retained-owner GC checks.

## Measured costs

Two fresh VMs per variant measured forced pieces with shared ordinary String payloads.

| Pieces / coder | First-read allocation, baseline → candidate | Minimum added construction | Final index size |
|---|---:|---:|---:|
| 4 / Latin-1 | 544 → 0 bytes | 32 bytes | 32 bytes |
| 4 / UTF-16 | 1,072 → 0 bytes | 32 bytes | 32 bytes |
| 1,024 / Latin-1 | 135,184 → 0 bytes | 8,320 bytes | 4,112 bytes |
| 1,024 / UTF-16 | 270,352 → 0 bytes | 8,320 bytes | 4,112 bytes |

Repeated reads allocated zero in both variants. Candidate repeated-read medians were
higher in all 24 many-piece pairs and 14 of 24 small-piece pairs. Shared-host timing
variation prevents a general speed claim. Retained shallow sizes exclude source
payloads; keeping their owners reachable remains a lifetime tradeoff. RSS/PSS was
unavailable; VM-exit NMT summaries do not establish per-builder native retention.

## Product gates remain open

The closed UTF-8 image attempt failed under unchanged `-Werror`: 288 warnings were
reported; the displayed 100 missing documentation tags came from unchanged
`SortAlgorithm` and `Sorts`. Remaining native work was stopped after the compiler
failure; no complete terminal `make` exit is inferred. No successful image or
`#flagOn`, native-storage, OS-interning or JNI-lifetime qualification exists.
Full reactor, candidate CI and final target acceptance remain pending. The active
phase is **String**, with no family or repository completion claim.
