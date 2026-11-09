<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->
# Spring 6.2 collections slice: CompositeIterator array storage (successor of spring-core-6.2.0-collections-20261018)

Successor receipt for one changed source and four new test files of `implementations/spring-core-6.2.0-collections`.
The 20261018 receipt stays unchanged as history; its row for `CompositeIterator.java` describes the bytes before this change.

- `CompositeIterator.java`: the M3 array-storage edit of hsoliwal/spring-framework `m3/array-storage-20261019` (made on
  7.1.0-SNAPSHOT), derived for the relocated 6.2.0 copy by a 3-way merge (ours = 6.2.0 postimage, base = fork preimage,
  theirs = fork postimage, all relocated to `com.m3.collections.spring`). Adaptations: one Javadoc `@author` conflict
  resolved to the 6.2.0 lines, jspecify `@Nullable` dropped (the slice has no jspecify), a contributor line added.
- Tests: `CompositeIteratorArrayStorageTests`, `M3ArrayStorageOracle` (compiles the 6.2.0 preimage at runtime and
  compares retained arrays and callback histories), `M3StreamProviderContractTests`, and the preimage resource.

Gates: slice main `javac --release 17 -g` (spring-core 6.2.0 on the classpath) 0 errors, the same 7 warnings as the
base, 89 classes; the 4 test classes 7/7; Spring's own `CompositeIteratorTests` of tag v6.2.0 (relocated) 5/5 on the
base and on the derived class.

Binding paths: `target_path` is relative to `implementations/spring-core-6.2.0-collections` (the predecessor binds main
sources only, relative to its `src/main/java`); `source_path` is relative to the donor repository root.
