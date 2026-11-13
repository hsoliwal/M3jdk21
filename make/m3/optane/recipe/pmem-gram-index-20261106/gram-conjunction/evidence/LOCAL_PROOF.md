<!-- SPDX-License-Identifier: Apache-2.0 -->
# Local executable qualification — 2026-11-05

Parent source branch PR #15431 at `87acd7a0fb85fe47ad082de97270747b52603024`. Confirmed original Java Git blob `916a33c1fb40b5a173ff57394fa4922c0deb0a33`, original JUnit blob `2668284f75d200a8e917c11628e4679d2e9b1854`. Full original source preimages, exact afterimages and SHA-256 manifest are retained.

**Observed PASS, isolated Java21 only:** original and enhanced code compiled with `javac --release 21 -Xlint:all -Werror` against first-party API-compatible PMEMSDK test stubs. Each variant passed original deterministic 3,887-assertion test. Independent first-hit brute-force oracle executed 85,005 assertions on each variant across 20,000 source entries; matching file indices and byte positions had identical SHA-256 `d81690f7723fa478d04cca40d51ea20c6023a9531c43ed1ab7b0463ff8ccff18`. Independent oracle JIT and interpreted JVM outputs matched. Both variants compiled to identical `javap -p` descriptor surfaces. New candidate count was 207 versus original 10,103 with 207 identical exact matches. Six authored JUnit-shaped fixture methods passed with API-compatible test-only stand-ins, including creation of 20,000 tiny files.

**NOT RUN:** actual complete Maven/OpenRewrite Recipe.run and real Jupiter engine; original FILE/POSIX JNI backend and Java/C17/JMOD/sanitizer integration; 1GB mapped workload/RSS/throughput; production quality reports, actual 20k Chrome inference calls, full canonical provenance and result-only public product receiving. A source/template hash or mock test must never be treated as proof that the native or model gates succeeded.

The isolated proof requires `M3_LIMITED_STANDIN_PROOF=1` only when missing the full original public corpus source; GitHub Actions and complete checkouts must leave that override unset.
