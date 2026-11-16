## Apache Commons Collections (development state after release 4.5.0)

### Origin

The Java sources under `providers/classes/jdk/m3/collections/apache` of the module `jdk.m3.collections` are derived
from Apache Commons Collections (https://github.com/apache/commons-collections): 363 of the 367 main sources of the
copy M3JDK holds in `m3/collections/implementations/commons-collections-4.5.0`. That copy is the fork
https://github.com/hsoliwal/commons-collections at commit `4000c0a3648d`, except three classes it holds in M3JDK's own
version (`queue.CircularFifoQueue`, `iterators.ArrayIterator`, `iterators.ArrayListIterator`). The fork follows the
library's development branch after release 4.5.0: its release notes are those of 4.6.0 and sixteen sources carry
`@since 4.6.0`. It is not the 4.5.0 release. Apache Commons Collections is licensed under the Apache License,
Version 2.0.

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
- four sources are not taken (see below);
- `@SuppressWarnings` was added to 135 top-level types for the lint categories the JDK build reports as errors;
- documentation: one `@see` to a class that is not taken was removed (`list.AbstractLinkedListJava21`), and the two
  list items for classes that are not taken were removed from the package description of `list`;
- three files lost an empty line at their end (`bloomfilter.CellExtractor`, `comparators.TransformingComparator`,
  `map/package-info.java`).

Compiled before and after these changes, every one of the 601 classes has the same members and the same instructions.

### What is not here, compared with the library

- `ArrayStack`: this module owns a class of that name in `share/classes`, and it stays. It has the library's eight
  members with the same signatures, but it is final and extends `AbstractList`, where the library's class is open and
  extends `ArrayList`: subclassing it, using it as an `ArrayList` and the `ArrayList` serial form are not available.
- `list.AbstractLinkedList`, `list.CursorableLinkedList` and `list.NodeCachingLinkedList` do not compile against the
  Java 21 API: their `boolean addFirst(E)` and `boolean addLast(E)` clash with the `void` methods `List` has since
  Java 21. `list.AbstractLinkedListJava21`, the library's own replacement for the first, is here.
  `CursorableLinkedList` (cursors that survive modification) has no replacement in this module. For
  `NodeCachingLinkedList` the module has a class of that name in the root package `jdk.m3.collections.apache`: final,
  the same three constructors, none of the library's protected cache hooks.

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
