<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 DOOP parent-owned mask/offset views (stacked on #1093)

Canonical Synexia source recipe: [#12100](https://github.com/hsoliwal/com.synexia/pull/12100).
Parent receiver: [#1093](https://github.com/hsoliwal/M3jdk21/pull/1093).
Original backing `M3LongCollection`, late filtering, sorted snapshots,
`M3LongDag`, virtual collectors and existing JNI mask compactor are **unchanged**.

`M3LongMaskRows.from(owner, byteFlags).requireFlags(3).sourceRange(100, 500)`
creates an immutable plan over absolute **original source ordinals**. The byte
flags are cloned once and shared among derived plans; the source still owns
primitive payloads and object lifetime. `view()` creates a single read-only,
late-bound primitive collection facade; `dag()` exposes the existing DAG with
filter/map, bounded sorted snapshots, and virtual collectors/accumulators.

`selectedSourceRows(Backend)` is an explicit O(N) index/materialization boundary
with O(N) temporary primitive row/flag inputs. It delegates to existing
`M3IndexMaskCompactor.select` for JAVA or NATIVE_REQUIRED. Neither a new JNI
symbol nor Java callback-from-native is introduced. Parent cardinality changes
are refused; same-count mutation follows the parent's iterator rules. This is
not an object-generation-proof full64 mask or a transactional snapshot.

GoF: Adapter/Decorator, Strategy, Iterator, Proxy, Composite, Flyweight are
structural roles; 23 original GoF designs remain 5 creational + 7 structural
+ 11 behavioral. Broader cross-family category catalogues remain in Synexia.

Complete checkout commands:

```
mvn -f m3/collections/qualification/m3-long-mask-rows-20261017/pom.xml verify
sh m3/collections/qualification/m3-long-mask-rows-20261017/verify.sh
mvn -f m3/collections/pom.xml test
```

Do not promote without actual SDK, module, native and jtreg passes. **DRAFT/HOLD.**
