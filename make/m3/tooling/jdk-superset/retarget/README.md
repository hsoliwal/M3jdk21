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
| `tools/donor_overlay.py` | DONOR_OVERLAY v2: the custody every donor-overlay recipe uses. Each upstream file stays byte-identical under `upstream-excluded/`; the overlay is pinned with its base and its recipe in `PROVENANCE.json` and `OVERLAY_SOURCES.json`; `verify_source.py` re-derives each overlay; the relocated (lib-intake) copy gets the name-mapped overlay. `--check` writes nothing |
| `tools/maven_gate.sh` | builds a donor pom on master and on the branch, runs `verify_source.py` with the jar, and compares the jars |
| `tools/base64_to_jdk.py` | BASE64_TO_JDK v1: HdrHistogram's `Base64Helper` calls `java.util.Base64` directly (`make/m3/evidence/java-base/BASE64_HDRHISTOGRAM.md`) |
| `tools/jgit_base64_to_jdk.py` | JGIT_BASE64_TO_JDK v1: JGit's `util.Base64` encodes with `java.util.Base64`, and decodes canonical input with it; other input keeps JGit's lenient loop (`make/m3/evidence/java-base/BASE64_JGIT.md`) |

A recipe module defines `RECIPE_ID`, `TOOL_REL`, `DONORS`, `TARGETS`, `overlay(original, upstream)`, `POM_TEXT`,
`README_TEXT`, `MODIFICATIONS` and `RELOCATED`, then calls `donor_overlay.run`. `overlay()` stays in the recipe file,
because `verify_source.py` pins that file's sha256 and calls it. v2 adds optional hooks:
- `REMOVED`: targets whose source goes;
- `POM_EDITS`, `POM_FORBIDDEN`: pom regex removals and strings that must not remain;
- `provenance_update(provenance)`: edits the donor's provenance record;
- `RELOCATED_EDITS`: edits to relocated files;
- `after(check, changed, custody)`: one step after all donors, such as relocking a module pack.

## Recipes in other lanes

Both also run on DONOR_OVERLAY v2:

- `number-parse/tools/jackson_numbers_to_jdk.py` (JACKSON_NUMBERS_TO_JDK v2): Jackson parses and renders numbers
  with java.base. It uses every v2 hook; `after` relocks the jackson module pack.
- `hashing/tools/checksums_to_jdk.py` (CHECKSUMS_TO_JDK v1): commons-codec's CRC-32 and CRC-32C delegate to
  `java.util.zip`.

Moving them changed only the pins of their own sha256: `recipe_sha256` in `PROVENANCE.json` and
`OVERLAY_SOURCES.json`, the record digests that follow from it, and the jackson pack's lock entry for jackson-core.
No overlay, pom, README or `verify_source.py` byte changed (`make/m3/evidence/java-base/donor-overlay-v2/AUDIT.txt`).
FDP_DELEGATES_TO_JDK (retired with the FastDoubleParser copies) was the last recipe with its own custody code.
