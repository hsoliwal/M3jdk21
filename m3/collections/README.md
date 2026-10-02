# M3 primitive collections foundation — WP-02 / WP-24 seed

Status: explicit Route-A primitive collection foundation. This module is **not** a replacement for
`java.util` and does not modify `java.base`.

It ports/adapts the dependency-minimal primitive mechanics already present in Synexia so later JDK
collection packets can share proven scalar/storage atoms instead of re-inventing them inside each
public collection implementation.

## Source pins

Primary source baseline: `hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0`.

The zero-object ring arithmetic additionally uses the reviewed additive atomization from Synexia
PR #7675 head `8fa47689547a1efeabbb35d2caf1683d902a87e4`; that branch relationship is explicit in source
headers and migration mapping and is not represented as merged into `develop`.

Ported/adapted capabilities:

- primitive long collection/iterator/queue/deque contracts;
- packed power-of-two long deque;
- primitive long-to-long map contract;
- open-addressed long-to-long hash map;
- shared capacity, ring and hash probe/state arithmetic.

## Representation

`M3PackedLongDeque` retains one `long[]` plus scalar head/size fields. Queue/deque operations are
amortized O(1); growth allocates one replacement lane and linearizes the logical ring.

`M3PackedLongLongHashMap` retains `long[] keys`, `long[] values` and `byte[] states`, plus scalar
size/used/capacity fields. It allocates backing lanes lazily on first insertion and uses linear open
addressing with explicit tombstone compaction. It retains no `Map.Entry` or boxed `Long` per mapping.

These primitive APIs do **not** prove boxing elimination for generic `Map<Long,Long>` or
`Collection<Long>` boundaries.

## Build and differential verification

Set `M3_JDK` to JDK 21 and run:

```bash
bash m3/collections/build.sh
```

The script compiles with strict lint, builds the module jar, compiles the standalone differential
suite, and runs it in normal and `-Xint` modes.

The initial local execution on OpenJDK 21.0.11 passed 1,006,642 checks per mode. The suite compares
randomized deque behavior with `ArrayDeque<Long>` and primitive-map behavior with `HashMap<Long,Long>`
for the overlapping semantics, while separately checking primitive-only operations and failures.
See `evidence/local-jdk21-20261002.json`.

## Scope boundary

This packet deliberately does not yet:

- replace `ArrayDeque`, `HashMap` or any other JDK public class;
- claim generic collection API, view, iterator, serialization or subclass compatibility;
- add concurrency or Java Memory Model claims;
- benchmark throughput or retained whole-heap bytes;
- claim the source-side PR #7675 is merged;
- install an M3 owner into `java.base`.

The next collection packet can use these atoms while separately proving the exact contract of a
specific JDK owner.

## Reusable transformation recipes

The executable source-bound installer is `recipe/apply.py` with exact postimage hashes and pinned
`ArrayDeque.java` / `HashMap.java` no-change guards. Its focused Python unit tests were executed.

A Maven/OpenRewrite equivalent is authored under `openrewrite/`:

- `M3InstallPrimitiveCollectionsRecipe`
- exact Java postimage resources for the complete production module
- guard/refusal/fixed-point tests
- a declarative `META-INF/rewrite` recipe

The OpenRewrite recipe is deliberately reported as **authored but unexecuted** in this packet because
Maven was unavailable in the execution environment. Its existence is not counted as a passed Maven
or OpenRewrite gate. A later environment with Maven should execute `mvn test` in
`m3/collections/openrewrite` and bind that result to the exact recipe/postimage hashes before
advancing that gate.
