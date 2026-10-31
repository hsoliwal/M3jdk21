<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Indexed Eclipse TreeSortedMap source custody

Receive the exact source and proof packet from https://github.com/hsoliwal/eclipse-collections/pull/11 (commit 7ead4f8b958b498e3de691dfe70f9efeef97edad, tree 8e548f00c6da0e3077b21ffca4435233f0ed3de2). The source manifest records every UTF-8 payload's byte size, SHA-256 and Git blob identity.

The existing Eclipse TreeSortedMap owner uses dynamic key/value/link arrays, integer physical slots/free links, and live/color bit masks. Entries are API projections. This is an actual backing-store implementation in the Eclipse fork.

Disposition: **PENDING_SOURCE_CUSTODY_ONLY**. These files are outside compiled source roots. Runtime receiving and canonical Synexia Maven/OpenRewrite crate/registration/execution remain pending. The pure source recipe demonstrates exact forward/inverse/fixedpoint/refusal guards; it does not establish that admission.

Preserve the ordered String -> arrays -> collections -> AST/compiler -> remaining families sequence. This packet advances no phase and adds no JNI symbols, Maven dependencies, installed providers or runtime modules. Full EC14/Java17/module/OSGi/installed-image/resource qualification and complete five-library coverage remain open.

The original Eclipse owner retains EPL/EDL licensing. Attributed new leaves, tests and recipe expression retain their Apache-2.0 notices. Retain the supplied Apache, Eclipse and donor notices; the recipe license does not relicense donor payloads.
