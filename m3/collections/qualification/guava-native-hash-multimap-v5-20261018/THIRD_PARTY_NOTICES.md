<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Attribution and license custody


## Native HashMultimap array and API-projection cache increment

Retained Guava 33.4.8 HashMultimap, Apache-2.0, source blob 719c0b40f66e87b92060cf074d067912e452e6f6. Original Guava author and license notices are preserved. The private value-set membership pattern adapts M3LinkedHashSet blob e1ac806d1ebe8aac1b9efe7bcfbaa3f675e7d0d6; the unchanged M3LinkedHashMap blob bc998ec2e65e1997de0e95b138818d04308a5105 supplies the existing array/index tables.

The primitive M3LFUCache adaptation derives from hsoliwal/M3jdk21@6ae20943e2295e6120c9666a04dc1f2be29f4873, blob 315fad8a68c1af45d7e23bdded687bea3d6537e8, Apache-2.0, Copyright 2026 Hitesh Soliwal and Contributors. Its use here is limited to immutable pair API projections; live entry/count projections remain open. M3-HASH-MULTIMAP-NOTICE.txt is copied byte-for-byte from the qualified artifact.

Canonical custody: https://github.com/hsoliwal/com.synexia/tree/c7a2de8816d2097c73125bc4b8e95227b3a83605/synexia-openrewrite-recipes/recipe-crates/guava-native-hash-multimap-v5-20261018. Existing Apache-2.0 build dependencies remain external. No challenge-platform submitted solution was copied. JMH/jopt-simple/Commons Math remain external benchmark-only dependencies with their original licenses; no dependency implementation is included in the provider.

Original Apache-2.0 text is retained at hsoliwal/guava m3-jre21/LICENSE, Git blob d645695673349e3947e8e5ae42332d0ac3164cd7; this companion copies no production Java.
