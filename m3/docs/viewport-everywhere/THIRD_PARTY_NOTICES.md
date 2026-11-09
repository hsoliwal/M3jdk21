<!-- SPDX-License-Identifier: Apache-2.0 -->
# Viewport everywhere: sources, licences and notices

## Own sources (copied by recipe)

- **hsoliwal/eclipse.platform.swt** `553eefd41d9961d303c41e0e4e181712e64a1e45` (integration branch
  `m3/viewport-integration-20261016`): the `Virtual*`/`Viewport*` classes listed in `FOSS_REUSE_DECISION.tsv`.
  At source they carry `SPDX-License-Identifier: EPL-2.0`; every commit touching them is by Hitesh Soliwal. Their
  copies in `m3/collections` and `m3/viewport-core` carry Apache-2.0 under the record in `RELICENSE_RECORD.md`.
  `PackedHeapGrowth` is already `EPL-2.0 OR Apache-2.0` (`m3/docs/m3-item-residency-explicit-relicense.md`).
- **hsoliwal/com.synexia** `af4494b143270d03cf4d3f33d1e7b4f84325f5dd`:
  `synexia-primitives/src/main/java/com/synexia/primitives/LongOrderStatisticMultiset.java` (Apache-2.0,
  Copyright 2026 Hitesh Soliwal and contributors), the origin of the AVL lanes in `M3OrderStatisticForest`.
- **hsoliwal/nebula** `eda708d14c0ff8aa5a433db2bfe01a64a4a4b655` (wave 3): `GridRowTree`, `GridSelectionList`,
  `GridCellSelectionList`, `Apache-2.0 OR EPL-2.0`.

## Third-party (pattern only or wrapped; no code copied)

| Project | Licence | Use |
|---|---|---|
| JetBrains/intellij-community | Apache-2.0 | adapter jar compiled against public APIs (phase 2); patterns |
| arotenberg/swingx | LGPL-2.1 | patterns only |
| eclipse-rap/org.eclipse.rap | EPL-2.0 file headers (repository LICENSE file: EPL-1.0) | patterns; adapter design |
| JetBrains/skija, JetBrains/skiko | Apache-2.0 | binding behind the M3 renderer adapter (render-modes branch); patterns |
| openjdk/jfx | GPL-2.0 WITH Classpath-exception-2.0 | patterns only |
| glazedlists/glazedlists | LGPL-2.1 OR MPL-1.1 | pattern confirmation only |
