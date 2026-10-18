<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# RX7 receive-result bootstrap recipe SDK

This source-only, unadmitted candidate reuses the immutable qualified cap SDK `e89f12783efeb9b6302aa0d02219651c8aeb9b5d` and the eight exact receive text descriptor/resource files from `2711cbfe44896502aa31ecf854de41f571aa3a13`. Physical borrow-first scope and FOSS decisions precede implementation at `d2cbc85192d09abc0548b7f825f86cddff44a224`.

Canonical home: `synexia-openrewrite-recipes/tasks/regex-v7-cold-free-projection-20261007/codec-receive-bootstrap-20261018/sdk`. A separately reviewed bootstrap receiver branch may place the same SDK tree at the existing `m3/migration/rx7-codec-cap-20261018/sdk` path with its existing external binding. The active launcher/workflow, original branches and qualified packet remain unchanged. External verification must bind the canonical source tree and receiver tree; the old launcher does not establish full source/task admission.

Every donor Java engine, Java descriptor/manifest/fixture, POM, license and notice remains byte-exact. Both old text crates/descriptors and their qualified launcher/workflow postimages remain byte-exact, keeping the old launcher's template binding valid. The original three test method names, Java export property, exact two Java exports and original Java success marker remain unchanged. The imported FOSS ledger is preserved donor history; this candidate's borrow/gap record is the physical foundation above.

The existing third method calls one parameterized qualification helper for the original cap text pair and then the receive text pair. Each pair retains all partial states, orders, fixed points, source drift/missing/duplicate refusals and marker checks. The receive full-BEFORE forward case executes the named composite recipe and captures only its actual two Results. Only those Results are exported; resource templates supply expectations rather than emitted bytes. The inherited missing/drift Result controls must identify their precise refusal before output exists.

The separate owned `${m3.build.root}/text-recipe-results` directory contains the two exact Result files at their receiver-relative paths plus `receipt.tsv`. It remains disjoint from the source and Java export roots, uses fresh nonlink paths, bounds each data file to64KiB and receipt to4KiB, limits the bundle to128KiB, checks the reserve and verifies the exact closed eight-node tree with bounded byte readback.

After readback, only the observed byte arrays are serialized into one buffered stdout carrier. There are exactly three LF-terminated TSV records and no start/end or JSON envelope:

```text
M3_RX7_TEXT_RESULT_B64_V1<TAB>path<TAB>sha256<TAB>bytes<TAB>base64<LF>
```

Fields are schema literal, exact relative path, lowercase SHA256, canonical decimal byte length and standard padded single-line Base64. Records are sorted for `.github/workflows/m3rx7-v7-image-codec-20261014.yml`, `m3/runtime-integration/check-m3rx7-v7-image-codec.sh`, then `receipt.tsv`. All three records together must fit64KiB before any are printed. Receipt schema is `M3_RX7_TEXT_RECIPE_RESULTS_V1`; it records the named full-BEFORE origin, both before/after hashes and byte counts, exporter-only non-application and hosted receive qualification NOT_RUN. No arbitrary files, environment data or old text payloads enter the carrier.

The unchanged old collector retains Maven stdout and Surefire XML. Its128KiB Maven-log,256KiB XML,2MiB proof,512MiB aggregate output,8GiB-after-planned-output reserve, deadlines and process supervision remain authoritative. Prior4228B Maven-log/15855B XML observations support fit only; future growth must still fail closed. The old hosted receipt proves only the existing cap runtime lane. A separate audit must verify all three named tests and the complete run, compare the three carrier records in retained stdout/XML, validate decoded bytes/receipt against canonical receive descriptors/manifests/pins and distinguish actual recipe output from installed control behavior.

Bootstrap Maven, actual carrier emission and complete hosted bootstrap qualification are NOT_RUN. The eight receive input-state controls and four receive fixture checks are still embedded candidate text and are not executed by bootstrap qualification. Actual EveryModule task/donor/context admission remains NOT_RUN and gates every future control Result application. After that gate, qualify receive PRE before receiving the two Java outputs, then qualify POST. No control or product output is applied by this SDK candidate; no global phase, full source/JDK/JNI/performance or complete migration admission is claimed.
