<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Coordinated shared javac publication

Publish the qualified compiler/source-seal and parsed-plugin-query leaves, the
optional Gradle provider, and the Maven source restoration as reviewable draft
pull requests. Preserve all receiving and canonical engine history. Do not merge,
squash, rebase, force-update, delete files, or promote broader build gates.

Production outputs must be the already-qualified Maven/OpenRewrite SDK results.
Canonical recipe datasets, stage receipts, source pins and this coordinating
record are evidence; public custody mirrors must reproduce those bytes exactly.
No compiler source, test, POM, SPI or recipe engine is hand-edited during packaging.

The compiler receiving order is m3jdk21-seals followed by
m3jdk21-plugin-selection. The independent gradle-provider-20261018 dataset adds
its optional artifact under the existing M3 build-support owner. The Maven
restoration remains a separate two-Java-file source leaf on its pinned master.
Existing custom Gradle SPI/hooks already survive the pinned master, so the missing
provider is supplied in M3JDK's documented provider location.

The canonical develop branch advanced through unrelated additive merges while
qualification ran. All bound recipe owners and task paths remain unchanged.
Publish new evidence on the observed successor only after verifying no task-path
or recipe-owner overlap; retain the original qualification revision as provenance.

Final runtime evidence is bound to descriptor-qualification-02. This is a linked
Java 21 compiler-source qualification image with actual compiler module service
metadata and supported hashes, not a full native JDK source build. Native Maven
reactor qualification, actual Gradle SPI/provider qualification, and full Gradle
worker/daemon execution remain separate gates. No benchmark or cross-process live
AST reuse claim is admitted.
