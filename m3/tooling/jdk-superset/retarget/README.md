<!--
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
-->
# Vendored libraries call java.base

The JDK owns one fast implementation, and every consumer uses it. When a vendored library carries its own copy of
something java.base provides, or reaches java.base indirectly (by reflection, or through a compatibility fallback), a
recipe here keeps the library's API and points its implementation at java.base.

## Tools

| Tool | Role |
|---|---|
| `tools/donor_overlay.py` | DONOR_OVERLAY v1: the custody every recipe here uses. Each upstream file stays byte-identical under `upstream-excluded/`; the overlay is pinned with its base and its recipe in `PROVENANCE.json` and `OVERLAY_SOURCES.json`; `verify_source.py` re-derives each overlay; the relocated (lib-intake) copy gets the name-mapped overlay. `--check` writes nothing |
| `tools/maven_gate.sh` | builds a donor pom on master and on the branch, runs `verify_source.py` with the jar, and compares the jars |
| `tools/base64_to_jdk.py` | BASE64_TO_JDK v1: HdrHistogram's `Base64Helper` calls `java.util.Base64` directly (`m3/evidence/java-base/BASE64_HDRHISTOGRAM.md`) |

A recipe module defines `RECIPE_ID`, `TOOL_REL`, `DONORS`, `TARGETS`, `overlay(original, upstream)`, `POM_TEXT`,
`README_TEXT`, `MODIFICATIONS` and `RELOCATED`, then calls `donor_overlay.run`. `overlay()` stays in the recipe file,
because `verify_source.py` pins that file's sha256 and calls it.

## Earlier recipes of the same kind

These carry their own copy of the custody logic:

- `number-parse/tools/jackson_numbers_to_jdk.py` (JACKSON_NUMBERS_TO_JDK v1, v2): Jackson parses and renders numbers
  with java.base.
- `hashing/tools/checksums_to_jdk.py` (CHECKSUMS_TO_JDK v1): commons-codec's CRC-32 and CRC-32C delegate to
  `java.util.zip`.

DONOR_OVERLAY v1 reproduces CHECKSUMS_TO_JDK v1's output byte for byte
(`m3/evidence/java-base/base64-hdrhistogram/replay_checksums.py`). Moving that recipe onto it changes the recipe's
sha256, so its overlays would need re-pinning; that is a separate change.
