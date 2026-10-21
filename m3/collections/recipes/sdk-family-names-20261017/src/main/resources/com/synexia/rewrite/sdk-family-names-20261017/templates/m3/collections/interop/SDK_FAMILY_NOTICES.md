# SDK family provenance and notices

Original Synexia/M3 additions in this packet: Apache-2.0, copyright 2026 Hitesh Soliwal
and contributors. The existing M3 source headers and license are retained. OpenJDK files
are unchanged and retain their existing license. This packet changes no `java.*` packages.

Libraries are normal optional-interop dependencies, not copied/relicensed implementation
source: Guava 33.4.8-jre, Eclipse Collections 11.1.0, Spring Framework 6.2.0, and newly
added Apache Commons Collections 4.5.0. Retain upstream artifact LICENSE/NOTICE files in
any binary distribution. Existing `THIRD_PARTY_NOTICES.md` remains applicable. No modified
Apache classes are shipped under Apache's package names; the two new subclasses have
`com.m3.collections.interop.M3Apache*ValuedMap` identities.

The Apache 4.5.0 `AbstractListValuedMap` source was inspected at the release tag
`rel/commons-collections-4.5.0`, blob `5b55bbcf34ce0c429b1a0146f9af87cb9cf8d3d0`.
Its public extension points are invoked, not copied. LeetCode, HackerRank and
GeeksforGeeks are problem/category references only; no submission/editorial code copied.
The source-seal build tooling is reused unchanged from the existing Synexia Apache-2.0
implementation. None of these dependency choices represents a legal or security review.
