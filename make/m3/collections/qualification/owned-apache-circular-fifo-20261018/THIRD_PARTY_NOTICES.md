M3CircularFifoQueue adapts the complete CircularFifoQueue implementation from Apache Commons Collections4.5.0, apache/commons-collections at7f7fb0244abc940a2e9dd28b67508c0483a58c3e, source blob5109b5754242cabb01873ad5d0ee80b7357614f5. The original ASF license header is preserved, and a modification notice identifies the namespace and transient state-bit changes. The accompanying Apache LICENSE.txt and NOTICE.txt are retained.

The only Commons production dependency is the BoundedCollection interface, blob6ab5f2916ed65ed26c4a373490675e10cd44df5c. No Commons implementation parent, delegate, factory or helper is used by the owned queue. The Commons4.5.0 jar is used as an API/fixture dependency and provider oracle during qualification.

The receiving engine is reused unchanged from Synexia M3HashPinnedJavaSnapshotRecipe, blobd58ca50dfaeb60f581b64f43545e688aca5c6530, SHA2563fc6e77a1394e292747d689a4a42a328b496000865aa390afe05d5571febd599, under its existing Apache-2.0 header. OpenRewrite8.17.1 and JUnit Jupiter5.10.2/Platform1.10.2 are test/tool dependencies.

LeetCode622, HackerRank Queue using Two Stacks, and GeeksforGeeks circular queue pages are algorithm-discovery references only. No submitted solutions or example implementation code from these sites is copied.
