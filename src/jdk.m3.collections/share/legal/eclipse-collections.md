## Eclipse Collections (development state 14.0.0-SNAPSHOT)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/eclipse` of the module `jdk.m3.collections` are derived
from Eclipse Collections (https://github.com/eclipse-collections/eclipse-collections) as carried and changed in the
fork https://github.com/hsoliwal/eclipse-collections at commit `969974880cb8f6296f902c56c0d2d15162bd5152`, a
development state of the library (version 14.0.0-SNAPSHOT), not a release. They are the main sources of the artifacts
`eclipse-collections-api` and `eclipse-collections`: 3,754 files.

- 922 are hand-written. With the package renamed, 918 of the fork's files equal the files M3JDK holds in
  `m3/collections/implementations/eclipse-collections-14.0.0-snapshot-00a668d6`; the other four
  (`impl.map.mutable.UnifiedMap`, `impl.map.sorted.mutable.IndexedRedBlackMap`,
  `impl.map.sorted.mutable.IndexedSortedMap`, `impl.set.mutable.UnifiedSet`) are taken in the version that copy holds.
- 2,832 are generated: the library keeps its primitive collections as templates and generates the sources at build
  time. They were generated with the library's own generator from the fork's templates (with the package renamed, the
  384 templates and the generator in M3JDK's copy are the same files) and are checked in here, because the JDK build
  does not run that generator.

The files here additionally carry the changes listed below.

### Licenses

Eclipse Collections is made available under the Eclipse Public License v1.0 or the Eclipse Distribution License v1.0
(its NOTICE: `SPDX-License-Identifier: EPL-1.0 OR BSD-3-Clause`). 3,750 of the files carry the library's header,
which names both licenses and their authors (Goldman Sachs and others); the headers are unchanged. Those files are
carried here under the Eclipse Distribution License v1.0, reproduced below. Nineteen of them also carry a line that
begins "Modified 2026 by Hitesh Soliwal"; those lines name no separate license.

Four files were written for the fork and carry their own header, "Copyright (c) 2026 Synexia, Hitesh Soliwal and
others", `SPDX-License-Identifier: Apache-2.0 OR EPL-2.0`: `impl.map.sorted.mutable.IndexedRedBlackMap`,
`IndexedRedBlackTree`, `IndexedSortedMap` and `IndexedTreeArena`. The Apache License notice for them is reproduced
below as well.

### Changes made by Synexia (2026-11-16)

The changes were mechanical. The tools that made them, their parameters and their proof are in the repository
https://github.com/hsoliwal/com.synexia under
`synexia-openrewrite-recipes/recipe-crates/inverse-package-relocation-20261116` (`inline_provider.py`,
`service_directives.py`, `INLINE_ECLIPSE.md`):

- the package `org.eclipse.collections` is named `jdk.m3.collections.eclipse`;
- the 478 annotations `aQute.bnd.annotation.spi.ServiceProvider` and `ServiceConsumer` were removed. In the library's
  release build they produce `META-INF/services` entries, which a named module does not read. The module descriptor
  declares the same 239 services instead (`uses` and `provides`). Without the `uses` the library's factory holders
  fail to initialise in a named module;
- `@SuppressWarnings` was added to or extended on 1,337 top-level types for the lint categories the JDK build reports
  as errors;
- documentation, for doclint: 24 links to types that are not visible became code text; 14 `@see` lines with such a
  reference, 7 `@param` tags that name no parameter and 7 lines holding only `<p>` were removed; in 4 files the header
  comment before `package` opened with `/**` and is a plain comment now;
- 50 blank lines were removed: the blank last line of 49 generated files and one doubled blank line in
  `impl.map.sorted.mutable.TreeSortedMap`. The generated files have Unix line ends here.

Compiled before and after these changes, every one of the 5,340 classes has the same members and the same
instructions. The class files of the 478 annotated types no longer carry the removed annotations.

### What differs from the library on the class path

- `impl.map.mutable.ConcurrentHashMapUnsafe` and the parallel iterables' `toSet`, `aggregateBy` and
  `aggregateInPlaceBy` need `sun.misc.Unsafe`; the module requires `jdk.unsupported` for them.
- The classes are defined to the platform class loader. `impl.block.factory.Functions.classForName()` resolves names
  through the library's own loader and therefore does not find application classes.
- A second provider for one of the 239 factory services anywhere on the class path makes that factory unusable
  ("Found multiple implementations"); this is the library's own rule.
- Serialized objects are not interchangeable with the library under its own package names.

### Eclipse Distribution License v1.0

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

### Apache License, Version 2.0 (the four files named above)

```
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
