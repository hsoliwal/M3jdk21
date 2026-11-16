## Apache Commons Collections 4.5.0

### Origin

The Java sources under `providers/classes/jdk/m3/collections/apache` of the module `jdk.m3.collections` are derived
from Apache Commons Collections (https://github.com/apache/commons-collections), release 4.5.0: 363 of the 367 main
sources of the copy M3JDK holds in `m3/collections/implementations/commons-collections-4.5.0`. That copy is the fork
https://github.com/hsoliwal/commons-collections at commit `4000c0a3648d`, except three classes it holds in M3JDK's own
version (`queue.CircularFifoQueue`, `iterators.ArrayIterator`, `iterators.ArrayListIterator`). Apache Commons
Collections is licensed under the Apache License, Version 2.0.

### Notice

```
Apache Commons Collections
Copyright 2001-2026 The Apache Software Foundation

This product includes software developed at
The Apache Software Foundation (https://www.apache.org/).
```

### Changes made by Synexia (2026-11-16)

The changes were mechanical. The tool that made them, its parameters and its proof are in the com.synexia recipe crate
`inverse-package-relocation-20261116` (`inline_provider.py`, `INLINE_COMMONS.md`):

- the package `org.apache.commons.collections4` is named `jdk.m3.collections.apache`;
- four sources are not taken. `ArrayStack` has the same name as a class this module already owns in
  `share/classes` (the module's class stays; it is final and extends `AbstractList`, the provider's extends
  `ArrayList`). `list.AbstractLinkedList`, `list.CursorableLinkedList` and `list.NodeCachingLinkedList` do not compile
  against the Java 21 API: their `boolean addFirst(E)` and `boolean addLast(E)` clash with the `void` methods `List` has
  since Java 21. `list.AbstractLinkedListJava21`, the library's own replacement, is taken;
- `@SuppressWarnings` was added to 135 top-level types for the lint categories the JDK build reports as errors, and one
  documentation reference to a class that is not taken became plain text.

Compiled before and after these changes, every one of the 601 classes has the same members and the same instructions.

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
