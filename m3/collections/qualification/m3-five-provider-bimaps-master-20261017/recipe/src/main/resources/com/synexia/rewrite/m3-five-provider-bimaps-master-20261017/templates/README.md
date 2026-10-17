<!-- SPDX-License-Identifier: Apache-2.0 -->
# Five-provider M3 SDK: current-master Apache Commons BidiMap

Product owner: M3JDK21. Canonical Synexia convergence recipe uses OpenRewrite and exact pinned SHA-256 inputs.

The current M3JDK21 master already has genuine JDK, Guava, Eclipse Collections, Apache and Spring SDK adaptations. The current Guava BiMap, its tests, Java base, native C, existing JDK/Eclipse/Spring code and vendor names MUST be preserved exactly. The current master is missing only the accompanying Apache Commons BidiMap implementation and factory.

Source of Apache M3ApacheBidiMap: previously submitted first-party M3JDK21 PR #1199, Git blob e3c3714fdd73c24d3e8ecdcca72f3cd0a20bf6eb. This source retains upstream ASF headers and original source adaptation notice. It extends the actual Apache Commons 4.5.0 AbstractDualBidiMap; the two real M3LinkedHashMap instances supply indexed forward and reverse storage. The upstream Apache contract controls duplicate value policy, inverse, MapIterator, and live views. This is not Guava BiMap with a renamed package. No per-element Entry/Node retained and no new JNI ABI.

Source preimage: M3JDK21 master 4598bfc398d3024e7ea67e42034e857a53a1b378. Recipe baseline: Synexia append-only develop ea172544e3ed6d5b67aa2248b821bfaa7c2efa9f. The six generated postimages are SHA256-sealed and installed only through a reviewed recipe. Do not patch generated product Java.

Real Apache vendor JUnit differential trace and original JDK/Guava/Eclipse/Spring tests included in target Maven workflow. Actual host Maven and full receiver ABI/contract/ JNI/ JDK tests are NOT YET green. No local Maven or true Commons binary was available and no vendor success is asserted. Keep target draft until real exact-head evidence, and do not claim full five-library SDK method coverage.
