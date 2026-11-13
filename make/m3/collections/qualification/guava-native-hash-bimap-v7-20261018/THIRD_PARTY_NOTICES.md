<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Attribution and license custody


## Native HashBiMap and API entry cache (2026-10-18)

The original JRE HashBiMap flow is retained from Guava 33.4.8, Git blob
`f5254b367e72e97067739c40c91bdfd94edd4847`. The array/index donor is Guava's
Android HashBiMap, blob `ad9138c8eca437bda700df63a4afa80e5abe474f` at baseline
`f06690fa3e874f65515e8fd338a74d636e2c792f`. Original Guava Apache-2.0 author
and license notices remain unchanged.

The private API-view cache adapts M3LFUCache from hsoliwal/M3jdk21 commit
`6ae20943e2295e6120c9666a04dc1f2be29f4873`, blob
`315fad8a68c1af45d7e23bdded687bea3d6537e8`, Apache-2.0, Copyright 2026
Hitesh Soliwal and contributors. The adaptation adds long generation and
creator-iterator lease coordinates while keeping cache bookkeeping primitive.
M3-HASH-BIMAP-NOTICE.txt and M3-LFU-API-ENTRY-NOTICE.txt preserve the exact
notices included in the qualified final artifact.

Canonical recipe/proof custody: https://github.com/hsoliwal/com.synexia/tree/03de1b62d58a98972e42eb09fae7635ff007c1e0/synexia-openrewrite-recipes/recipe-crates/guava-native-hash-bimap-v7-20261018.
Assembly 3.8.0, Plexus Archiver 4.10.4 and AntRun 3.2.0/Ant 1.10.15 are
Apache-2.0 build dependencies; their implementations are not copied here.
The canonical XML and Java recipe engines are unchanged.

Original Apache-2.0 text is retained at hsoliwal/guava m3-jre21/LICENSE, Git blob `d645695673349e3947e8e5ae42332d0ac3164cd7`; no production Java is copied into this companion.
