<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Existing-owner naming disposition

The existing target naming gate executed successfully on pinned master
33d2e6ae8048c436bd55b951d9f7dbfab2f3efcb; see NAME_GATE.log. Its three
queued migration examples remain queued. No naming plan or canonical map changes.

This pass refines private behavior inside the existing M3IndexedArray,
M3IntAlgorithms, CollectionViews, M3ArrayifiedIndexOps,
M3LongBitmapFilteredSnapshot and M3IndexMaskCompactor owners. It performs no
rename, package move, public/protected signature change, JDK backend promotion
or new JNI export. Existing-owner in-place changes are explicitly excluded
from the migration queue; they do not qualify a replacement java.util backend.

Source-built current-JDK, full jtreg/JCK/platform and ordered family promotion
remain independent gates. The target authority remains m3/docs/name-mapping.json.
