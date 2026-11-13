# Native viewport source received through the canonical SDK recipe

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

This continuation receives exactly one active native source in M3JDK21:
`m3/collections/src/main/c/m3maskviewport.c`. It is the actual OpenRewrite Result
from canonical Synexia commit `880d6cca109a9c4bb8f9eb0204138915763aba78`, dataset
`viewport-native-receive-20261022`. No alternate recipe engine, widget, collection
wrapper, public facade or payload owner is introduced. Java source is unchanged.

## What is now source-applied

The original `project` entry uses bounded 128-int input regions and retains full
validation, stable order, alias-safe selected scratch and one output write.
The same native file includes the qualified `projectPage` export, local-page-density
optimization and fixed 128-int small-page capture. The original product Java
class still declares only `project`: page JNI wiring is PENDING its Java-LST gate.
Do not describe the new native page export as a completed widget or Java integration.

Seven genuine SDK tests passed, including receiving the pinned current product C
preimage and zero-change replay. Actual received bytes were compiled and run, then
independently compiled with the unchanged original product Java kernel/declaration
and original page contract: 121,831 Java / 163,640 JNI checks per execution in
mixed and interpreted modes. The original and new candidate corpora, legacy ABI
and ASan/UBSan allocation/read-failure fixtures passed. Repeated counts are not
independent summed coverage. Exact observations and source seals are in RECEIPT.json.

The initial canonical host invocation timed out during interpreted Java testing.
Its partial logs remain. Only that incomplete test and subsequent ordered commands
were resumed; all source inputs stayed unchanged. A separate actual-product-owner
check subsequently completed uninterrupted with exit 0. This is not relabeled as
an uninterrupted full canonical run. Raw selected logs are committed; the complete
133-entry raw-log packet is hash-bound in the receipt and supplied with the archive.

## Reproduce canonical receiving

Use the existing canonical `native-leaf/qualify.sh` with explicit absolute paths to
Java21, Maven3.9.9, the verified offline dependency cache, a fresh output directory,
the canonical recipe owner and the original pinned product C before-image. The
qualifier checks source identity before creating evidence, executes the SDK, checks
one Result and zero-change replay, then compiles/runs the received result. Historical
before-images remain explicit; do not weaken their hashes to rerun against an already
received file. The before/after identities in RECEIPT.json are authoritative.

## Remaining acceptance

This review-branch source application does not install a JDK or advance the ordered
String -> arrays -> collections phase. Full Java-LST/nine-target receiving, wrapper
and provider suites, source-built JDK/HotSpot/jtreg, actual SWT/Nebula/GEF/RWT widgets,
GPU/theme/language/IME/accessibility, other platforms and comparative resources remain
pending. No throughput or total-memory improvement is claimed. Keep the PR draft.
Preserve existing rank-page work and all historical receipts; no merge or rebase.
