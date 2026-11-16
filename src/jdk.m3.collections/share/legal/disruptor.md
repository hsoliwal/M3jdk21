## LMAX Disruptor (development state 4.0.0-SNAPSHOT)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/disruptor` of the module `jdk.m3.collections` are derived
from the LMAX Disruptor (https://github.com/LMAX-Exchange/disruptor) as the fork
https://github.com/hsoliwal/disruptor has it at commit `c871ca49826a6be7ada6957f6fbafcfecf7b1f87` (2025-04-02), a
development state of the library (its build file says version 4.0.0, snapshot), not a release: all 70 main sources.
The Disruptor is licensed under the Apache License, Version 2.0. Copyright 2011 LMAX Ltd. and the authors named in the
file headers; the headers are unchanged.

### Changes made by Synexia (2026-11-16)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`):

- the package `com.lmax.disruptor` is named `jdk.m3.collections.disruptor`;
- `@SuppressWarnings` was added to 16 top-level types for the lint categories the JDK build reports as errors.

The other 54 files are, apart from the package name, the fork's files. Compiled before and after the second change,
every one of the 82 classes has the same members and the same instructions.

### License

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
