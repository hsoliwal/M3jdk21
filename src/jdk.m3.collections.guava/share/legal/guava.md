## Guava 33.4.8

### Origin

The Java sources of the module `jdk.m3.collections.guava` are derived from Guava
(https://github.com/google/guava), release 33.4.8, as carried and changed in the fork
https://github.com/hsoliwal/guava at commit `a26ada9e9503a5fdf1eff4d95c641fba68ce6560`: all main sources of the
`guava` artifact (JRE flavour) and of `failureaccess`. Guava is licensed under the Apache License, Version 2.0.
Copyright (C) The Guava Authors.

### Changes made by Synexia (2026-11-16)

The changes were mechanical. The tool that made them, its parameters and its proof are in the com.synexia recipe crate
`inverse-package-relocation-20261116` (`inline_provider.py`, `INLINE_GUAVA.md`):

- the packages `com.google.common` and `com.google.thirdparty` are named `jdk.m3.collections.guava` and
  `jdk.m3.collections.guava.thirdparty`;
- JSpecify annotations are the ones the JDK carries in `jdk.m3.annotations`;
- annotations of Error Prone and of J2ObjC were removed (1,493 uses, 470 imports). They are hints for external
  tools and have no effect at run time;
- `@SuppressWarnings` was added to 119 top-level types for the lint categories the JDK build reports as errors, and
  13 documentation comments were corrected for doclint (10 links to types outside the module became code text,
  3 heading levels).

Compiled before and after these changes, every one of the 1,936 classes has the same members and the same
instructions.

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
