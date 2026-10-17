<!-- SPDX-License-Identifier: Apache-2.0 -->
# Collection interoperability provenance

M3-owned additions are Apache-2.0; retain the existing M3 collection LICENSE/NOTICE and source attribution. OpenJDK licensing is unchanged and no OpenJDK implementation source is copied into this optional artifact by this packet.

M3SpringMultiValueMap adapts four allocation-bearing methods from Spring Framework MultiValueMapAdapter v6.2.0, Git blob `4c7c2f1d133cccef1eab060524a71cd3de4b45a9`, Apache-2.0. The source header retains the original 2002-2024 copyright and license and identifies the 2026 modifications. No other Spring method or utility is reimplemented here. AutoPopulatingList and MultiValueMap static bridges are used through public APIs.

Unmodified runtime dependencies retain their own upstream licenses and notices: `com.google.guava:guava:33.4.8-jre` (Guava, Apache-2.0); `org.eclipse.collections:eclipse-collections:11.1.0` and its API artifact (Eclipse project's EPL v1.0 and EDL v1.0 distribution notices); and `org.springframework:spring-core:6.2.0` and its dependencies (Spring, Apache-2.0). These artifacts are not shaded, copied wholesale, or relicensed as M3 source by this packet. Pinned coordinates are compatibility inputs, not assertions of the latest or recommended production versions.

LeetCode, HackerRank and GeeksforGeeks were consulted separately for problem/category evidence. No solution source was copied. The searchable donor-review.tsv records exact inspected Git source blobs, reviewed category pages, and rejected adapter assumptions.

The earlier Apache predicated-adapter delivery used the Apache Commons Collections 4.5.0 dependency (`org.apache.commons:commons-collections4:4.5.0`) retains the upstream Apache-2.0 license and NOTICE. `M3ApacheCollections` uses only its public `PredicatedList`, `PredicatedSet`, `PredicatedSortedSet`, `PredicatedMap` and `PredicatedSortedMap` decorators, with existing M3 storage delegates. No Commons implementation source is vendored or relicensed. Apache *predication* validates writes; it is not equivalent to Guava live visibility filters. The factory/test additions are independently authored under Apache-2.0, and the separately built optional interop module carries the dependency rather than `java.base`.

This successor upgrades that optional dependency to the reviewed 4.6.0 release and reruns the retained predicated-adapter tests.

## Five-provider array/count/flag extension (2026-10-17)

Guava `M3GuavaBiMap` adapts `AbstractBiMap` from exact commit
`f06690fa3e874f65515e8fd338a74d636e2c792f`, source Git blob
`26a2df528ed8957faa5417195810e9fa9c68d2c4`. `M3GuavaMultiset` adapts
`AbstractMultiset`/`Multisets` skeletal contracts from the same release; original
Guava notices are retained in those files. The unmodified Guava testlib is used as
an independent contract suite, with its original Apache-2.0 license.

Apache Commons Collections 4.6.0 is pinned to
`4107624de41aeed37985d2a069fad41992fd71a2`. Its protected multivalue and bidi
extension points are inherited over M3 owners. Count and legacy Bag algorithms
are adapted with source notices, while replacing boxed counter storage with the
M3 primitive count owner. `contract/apache-adaptation-lineage.json` in the
canonical packet records exact source paths, Git blobs, hashes and disposition.
The exact Apache LICENSE and NOTICE accompany this optional artifact in `licenses/`.

Eclipse MutableBag extends the public AbstractMutableBag hierarchy; Eclipse
OrderedMapAdapter is an unmodified runtime dependency. Eclipse source/license
provenance remains EPL-1.0 and EDL-1.0 as distributed upstream; this packet does
not relabel the Eclipse implementation as Apache-2.0. Both original license texts
are retained. Any adapted bag method identifies its donor header separately.

The new M3ObjectCounts integration reuses existing M3 directory, array-growth
and Java/JNI mask owners. No OpenJDK implementation body is copied into the new
count owner. Reference payloads remain in Java Object arrays managed by the GC.
Exact original donor licenses are in `licenses/`; source pins and the inspected
source/license hashes are in `five-provider-origin.json`.
