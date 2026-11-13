<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 CPU wave 3: exact product-source receiving handoff

Eight first-party Java 21 algorithm classes and two independent oracle fixtures are
added within the existing com.m3.tools.cpu JMOD owner. The M3JDK21 product is not
an adaptation layer. No Synexia Maven/OpenRewrite recipe code lives in this fork.

Finite-state CTL supports EX/EU/EG/AG/AF/AU on bounded totalized CSR; graph-
coloring CSP is an exact MRV backtracker, limited to 64 vertices and 16 colors.
The remaining bounded atoms cover sorted temporal joins, immutable Bloom
membership signals, pure FIFO multi-server simulation, guaranteed but nonminimal
pairwise categorical generation, static deny-overrides ACL projection, and a
pure event-time token bucket. All storage uses primitive arrays, offsets and
masks; APIs do not implement complete SMT, SQL, authorization, operating-system
rate limiting, distributed consensus or a full CTL/temporal language.

The strict standalone independent test oracles passed both JIT and -Xint:
M3_WAVE3_TEMPORAL_CSP_PASS checks=202532
M3_WAVE3_INDEXED_SYSTEMS_PASS checks=17102683
Total 17,305,215 checks *per mode*, not unique across both modes.

JMOD build, modified HotSpot/java.base/jtreg, Maven/OpenRewrite SDK recipe and
fixed point, platform and performance gates remain NOT_RUN. Keep draft, preserve
source-sealed Synexia custody and do not merge until the target gates pass.
