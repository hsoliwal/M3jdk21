## Eclipse Collections (development state 14.0.0-SNAPSHOT)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/eclipse` of the module `jdk.m3.collections` are derived
from Eclipse Collections (https://github.com/eclipse-collections/eclipse-collections) as carried and changed in the
fork https://github.com/hsoliwal/eclipse-collections at commit `969974880cb8f6296f902c56c0d2d15162bd5152`, a
development state of the library (version 14.0.0-SNAPSHOT), not a release. They are the main sources of the artifacts
`eclipse-collections-api` and `eclipse-collections`: 3,754 files.

- 922 are hand-written. 918 of them are, byte for byte, the files M3JDK holds in
  `m3/collections/implementations/eclipse-collections-14.0.0-snapshot-00a668d6`; the other four
  (`impl.map.mutable.UnifiedMap`, `impl.map.sorted.mutable.IndexedRedBlackMap`,
  `impl.map.sorted.mutable.IndexedSortedMap`, `impl.set.mutable.UnifiedSet`) are taken in the version that copy holds.
- 2,832 are generated: the library keeps its primitive collections as templates and generates the sources at build
  time. They were generated with the library's own generator from the fork's templates (the 384 templates and the
  generator in M3JDK's copy are the same files) and are checked in here, because the JDK build does not run that
  generator.

Eclipse Collections is made available under the Eclipse Public License v1.0 or the Eclipse Distribution License v1.0
(SPDX: `EPL-1.0 OR BSD-3-Clause`). It is carried here under the Eclipse Distribution License v1.0, reproduced below.
The file headers name both licenses and their authors (Goldman Sachs and others); they are unchanged. Sources written
for the fork carry their own headers.

### Changes made by Synexia (2026-11-16)

The changes were mechanical. The tools that made them, their parameters and their proof are in the com.synexia recipe
crate `inverse-package-relocation-20261116` (`inline_provider.py`, `service_directives.py`, `INLINE_ECLIPSE.md`):

- the package `org.eclipse.collections` is named `jdk.m3.collections.eclipse`;
- the 478 annotations `aQute.bnd.annotation.spi.ServiceProvider` and `ServiceConsumer` were removed. They make the
  library's build write `META-INF/services` entries, which a named module does not read. The module descriptor
  declares the same 239 services instead (`uses` and `provides`), so the factories are found as before;
- `@SuppressWarnings` was added to or extended on 1,337 top-level types for the lint categories the JDK build reports
  as errors;
- documentation, for doclint: 24 links to types that are not visible became code text; 14 `@see` lines with such a
  reference, 7 `@param` tags that name no parameter and 7 empty `<p>` lines were removed; in 4 files the header
  comment before `package` opened with `/**` and is a plain comment now;
- 50 blank lines that the removed annotations left behind were removed.

Compiled before and after these changes, every one of the 5,340 classes has the same members and the same
instructions.

### License

```
Eclipse Distribution License - v 1.0

Copyright (c) 2007, Eclipse Foundation, Inc. and its licensors.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

    Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    Neither the name of the Eclipse Foundation, Inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
