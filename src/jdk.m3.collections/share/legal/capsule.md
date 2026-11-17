## Capsule 0.7.2

### Origin

The Java sources under `providers/classes/jdk/m3/collections/capsule` of the module `jdk.m3.collections` are derived
from Capsule (https://github.com/usethesource/capsule) at commit `3e0da2d0cf7f0b6a9b9e54a7a18c6c36f4df6d2b`
(2026-09-10) of its branch `main`: all 42 main sources. That commit is seven commits after the release tag `v0.7.2`.
Three of the seven touch `core/PersistentTrieMap.java` (a change, its merge as pull request #40, and the revert of
that merge); in the end every main source is the same file as at the tag (the same blob ids), so the sources are
those of release 0.7.2. They are also byte for byte the 42 files of `capsule-0.7.2-sources.jar` in the maintainers'
repository https://releases.usethesource.io/maven/ (its SHA-1 as published there). The fork
https://github.com/hsoliwal/capsule (`1827df9d408b96ac012d86d72d9a582f8d7157d6`) is two commits ahead of that commit;
they change `pom.xml` only. The tests and the benchmarks are not taken.

Capsule is licensed under the BSD 2-Clause License. The text below is the repository's file `LICENSE`, without the
blank at the end of its first line. Every one of the 42 sources carries this header, and its text is unchanged:

```
Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
All rights reserved.

This file is licensed under the BSD 2-Clause License, which accompanies this project
and is available under https://opensource.org/licenses/BSD-2-Clause.
```

No source says that it was copied from other code. The library is not on Maven Central; its releases are published
in the maintainers' repository named above.

### Changes made by Synexia (2026-11-17)

The changes were mechanical (com.synexia repository, recipe crate `inverse-package-relocation-20261116`,
`provider_pipeline.sh`):

- the package `io.usethesource.capsule` is named `jdk.m3.collections.capsule`. With it the system property that
  `factory.DefaultSetFactory` reads, `<package>.Set.targetClass` (built from the class name), is
  `jdk.m3.collections.capsule.Set.targetClass`, and its default value names
  `jdk.m3.collections.capsule.core.PersistentTrieSet`;
- the header comment of each of the 42 files opened with `/**`, the opening of a documentation comment, where no
  documentation belongs; doclint, which the JDK build runs as errors, reads its `<` and `&` as HTML. Its first line
  is `/* *` now, which makes it a plain comment. The text of the header is unchanged;
- `@SuppressWarnings` was added to 39 top-level types in 26 files for the lint categories the JDK build reports as
  errors;
- documentation: in `SetMultimap` one `@throws UnsupportedOperationException, if not supported` lost the comma after
  the exception name;
- a trailing blank was removed from 4 lines in 4 files (the repository's rule for Java sources, `.jcheck/conf`).

No top-level type was moved to another file.

Compiled before and after these changes, every one of the 148 classes has the same members and the same
instructions.

### Exported packages

The library has no module descriptor; all ten packages are exported. The library marks no package as internal, but
`core.trie`, `util` and its subpackages are its implementation, exported here only because nothing says otherwise.
It marks the interfaces `SetMultimap` and `BinaryRelation` (with their `Immutable` and `Transient`) `@Experimental`,
"an experimental API that change at any time, without any stability or backwards compatibility guarantees" (source
retention: the compiled classes carry no trace of it), and `util.collection.AbstractImmutableMap` and
`AbstractImmutableSet` `@Deprecated`. In `core.trie`, the public method `MultimapResult.unchanged()` returns the
package-private `MultimapResultImpl`, as in the library.

### Known properties of the library, unchanged here

An independent test with random operations found these in the library and in this provider alike:

- `entrySet().contains(entry)` of the maps and multimaps calls `containsKey(entry)`, so it is `false` for an entry
  that is there, and `entrySet().equals(...)` with a `java.util` entry set is `false`;
- `Map.Transient.__put` with an equal value returns `null` instead of the old value for a key in a hash-collision node;
- `BinaryRelation.__put(key, value)` replaces the values of `key` forward but leaves `key` under the old values in the
  inverse.

### License

```
Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
