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

## RE2J1.8 internal fallback source

M3JDK21 carries the 19 Java production sources from google/re2j at
`57278921a609461c14d9cdb057d7aa9511c8f7ac` (tag `re2j-1.8`) in the private
`com.m3.fs.internal.re2j` package. The original Go and RE2J copyrights are
preserved in every file. The only source adaptations are package/import names
and SOURCE-retained serial-warning annotations on two existing declarations.
No RE2J binary or Synexia runtime service is required by the production module.
The unchanged upstream artifact is a test-only differential oracle.

RE2J has its own BSD-3-Clause terms; the M3FS Apache license does not relicense it.
The full terms follow so this notice remains sufficient when packaged in the
M3FS JAR, the OSGi bundle, or the linked module's legal directory.

```text
This is a work derived from Russ Cox's RE2 in Go, whose license
http://golang.org/LICENSE is as follows:

Copyright (c) 2009 The Go Authors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.

   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the
     distribution.

   * Neither the name of Google Inc. nor the names of its contributors
     may be used to endorse or promote products derived from this
     software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
