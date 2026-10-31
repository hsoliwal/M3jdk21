<!-- SPDX-License-Identifier: Apache-2.0 -->
# Collection interoperability provenance

M3-owned additions are Apache-2.0; retain the existing M3 collection LICENSE/NOTICE and source attribution. OpenJDK licensing is unchanged and no OpenJDK implementation source is copied into this optional artifact by this packet.

M3SpringMultiValueMap adapts four allocation-bearing methods from Spring Framework MultiValueMapAdapter v6.2.0, Git blob `4c7c2f1d133cccef1eab060524a71cd3de4b45a9`, Apache-2.0. The source header retains the original 2002-2024 copyright and license and identifies the 2026 modifications. No other Spring method or utility is reimplemented here. AutoPopulatingList and MultiValueMap static bridges are used through public APIs.

Unmodified runtime dependencies retain their own upstream licenses and notices: `com.google.guava:guava:33.4.8-jre` (Guava, Apache-2.0); `org.eclipse.collections:eclipse-collections:11.1.0` and its API artifact (Eclipse project's EPL v1.0 and EDL v1.0 distribution notices); and `org.springframework:spring-core:6.2.0` and its dependencies (Spring, Apache-2.0). These artifacts are not shaded, copied wholesale, or relicensed as M3 source by this packet. Pinned coordinates are compatibility inputs, not assertions of the latest or recommended production versions.

LeetCode, HackerRank and GeeksforGeeks were consulted separately for problem/category evidence. No solution source was copied. The searchable donor-review.tsv records exact inspected Git source blobs, reviewed category pages, and rejected adapter assumptions.

The additive optional Apache Commons Collections 4.5.0 dependency (`org.apache.commons:commons-collections4:4.5.0`) retains the upstream Apache-2.0 license and NOTICE. `M3ApacheCollections` uses only its public `PredicatedList`, `PredicatedSet`, `PredicatedSortedSet`, `PredicatedMap` and `PredicatedSortedMap` decorators, with existing M3 storage delegates. No Commons implementation source is vendored or relicensed. Apache *predication* validates writes; it is not equivalent to Guava live visibility filters. The factory/test additions are independently authored under Apache-2.0, and the separately built optional interop module carries the dependency rather than `java.base`.
