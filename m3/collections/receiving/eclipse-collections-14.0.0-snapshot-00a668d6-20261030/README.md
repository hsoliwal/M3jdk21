<!--
/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
-->
# Eclipse Collections receive: successor to fork master 07ba121f

This is the dated successor of [the 20261024 receive](../eclipse-collections-14.0.0-snapshot-00a668d6-20261024/). That
directory is kept unchanged. Its hash pins are history.

[The implementation directory](../../implementations/eclipse-collections-14.0.0-snapshot-00a668d6/) now holds fork master
`07ba121f146af93dd8cd21a179c1059da3f0fd23` of `hsoliwal/eclipse-collections` (tree `8ad16fd5b7eb`). The 20261024 receive
took prep commit `d7e0b3970e61`. Fork master is 5 commits ahead of it and 0 behind (fork PRs #14, #15 and #16), and those
commits change 6 files. All 6 are taken byte for byte from the fork blobs.

| File | Change |
|---|---|
| `eclipse-collections-testutils/.../impl/test/Verify.java` | Before comparing, the historical `org.eclipse.collections` golden streams in `assertSerializedForm` and `decodeObject` are mapped to the relocated class metadata. Payload bytes are compared unchanged. |
| `eclipse-collections-testutils/.../impl/test/VerifyTest.java` | 9 tests for that mapping, including mutation and alias rejection |
| `unit-tests/.../impl/list/mutable/FastListTest.java` | Serialized size is `132 + FastList.class.getName().length()` instead of the upstream 182: 185 with the relocated name |
| `tools/m3scale/M3ForkFilePass.java` | `Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)`: a tracked symlink is recorded as a symlink. It is no longer hashed through to its target. |
| `tools/m3scale/README.md` | Controller blob reference that goes with the change above |
| `m3/qualification/eclipse-release-20261029/EXECUTION.json` (new) | Fork receipt. It pins the before and after SHA-256 of the three serialization files, and they match this receipt (3 of 3). |

The fork versions of `pom.xml` and `m3/stream-interop/pom.xml` are not taken. The fork never had the install/deploy skips
from config successor A2, so the received blobs stay. Apart from those two POMs, the received tree equals the fork tree.
The expected subtree id is `e6e724517afb173a9c7aca4def40266d582c01e5`.

`FULL_SOURCE_BINDINGS.tsv` has the same nine columns as the predecessor and 3,481 rows. Compared with the predecessor,
5 rows have new after-blob, after-SHA-256 and after-byte values, and 1 row is new (`EXECUTION.json`).
`SOURCE_QUALIFICATION.json` records the predecessor files by path, size, SHA-256 and blob, the old and new hashes of each
file, and the checks.

The predecessor's `SOURCE_QUALIFICATION.json` pins its bindings file at `3041aefce474...`. That value is reproduced when
the two POM rows carry the prep-tree values. The file at base `bb66a8c16642` carries the A2 values for those rows instead,
so its own SHA-256 is `0230490e25c4...`. The predecessor is left as it is.

No JVM ran for this successor. The checks are hash checks only. Full Maven, OSGi and JPMS builds and the full donor
test suites remain open, as in the predecessor.
