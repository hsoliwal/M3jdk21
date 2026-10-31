<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# Dependency and redistribution notices

The M3FS Java and native adaptation is first-party Apache-2.0 source. Native
builds link to the caller's reviewed SQLite library. The optional FUSE projection
links to the caller's FUSE3 development/runtime libraries; neither dependency is
downloaded by the production CMake build.

SQLite's source distribution dedicates its code to the public domain. The tested
Linux runtime used Debian bookworm SQLite 3.40.1, with exact archive and shared
library hashes in the qualification receipts. The runtime-image wrapper copies
the explicitly supplied SQLite shared library and records its input and output
hashes. Preserve any notices required by the particular supplied distribution.

FUSE3 3.14.0 was used to compile the optional projection. Its upstream library is
LGPL-2.1-or-later; its utilities and other files retain their own upstream terms.
FUSE is not copied into the tested Java runtime image. Building the projection
does not establish that a FUSE mount was executed.

Eclipse Resources/Filesystem/Equinox are separate pinned EPL-2.0 runtime inputs
for the consumer tests, together with their OSGi/JNA dependencies. No Eclipse core
source is copied into M3FS. The consumer's POM records every resolved coordinate;
upstream notices remain attached to those artifacts.

JUnit, SQLite JDBC, SLF4J and Maven/OpenRewrite dependencies are qualification or
recipe inputs. The production M3FS Java module requires only java.base. Offline
test execution depends on a previously populated, verified dependency cache;
this task does not claim repository-wide source inlining of all test/tool inputs.

The linked image uses a stock Temurin Java 21 toolchain for this bounded module
qualification. OpenJDK files retain GPL-2.0 with the Classpath Exception and their
original image notices. This port does not relicense OpenJDK or qualify a complete
modified M3JDK21/HotSpot build.
