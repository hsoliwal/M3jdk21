<!-- SPDX-License-Identifier: Apache-2.0 -->
# Third-party and first-party provenance

The OpenRewrite SDK dependency is org.openrewrite:rewrite-core:8.17.1 under Apache-2.0.
JUnit Jupiter 5.10.2 is used only for actual named recipe tests; retain its upstream
Eclipse Public License 2.0 terms. Existing Maven plugins and notices remain unchanged.

This module directly reuses Synexia's Apache-2.0 M3GpuToolJoinRecipe
(1c1b1b568d369f688f6953161636bf36b484bf5a), M3GpuToolJoinManifest
(38df5f97bbeec8a8a8cfc77091d56bda925c07f2), and existing exact POM
(d79867bcc09ad6329e12418e15907e54b8ab025f). Thin adapters, three-method tests,
closure/report validators and constructor controls adapt the admitted backport SDK
e923d7cb1fe938489926ac670b0c4ee16bc91ab3, host
8b2f0dc1b85eb0ba755eaccb2752c14a985b5005 and runtime
4c64fbf724649bd9527e95b6456bcda842ec8086 at canonical commit
276b8676c3e9c189d520a49494ea0642b8ed22e9. Existing owner notices are preserved.
No unrelated source license is changed. The new qualification remains pending actual
execution; previous proof is not represented as proof of these new templates.
