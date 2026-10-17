# Collection traversal refinement contract

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

Inventory pins: M3jdk21 master 4e29dc9d4141261e8763fbe9ffd6a20e017b1aff; Synexia develop 892e3f6c51d78bc0948e8a8cf736055b4c70eafa.

Existing owners: M3FilteredLists live read-only list, M3IndexMaskWindow bounded mask selection, M3IndexMaskCompactor native loader. Preserve public descriptors, live backing/order/null/mutation refusal, cursor geometry and native symbols. Java predicates remain Java callbacks. Keep sorted views and Guava/Eclipse Collections/Spring/JDK contracts in their existing namespaces; Apache Commons remains a separate donor/adapter boundary. No universal SDK-completion or JDK phase-advancement claim.

Rejected candidate: delegating the read-only List iterator would avoid quadratic scans, but changes when a throwing predicate is evaluated and backing interference is detected. Preserve that owner in this contract-locked pass; do not silently change callback effects to obtain a speedup.

Pass 1: reuse the already inventoried M3 mask-window identities: contradictory required/excluded masks select nothing; zero masks copy one contiguous source window. Keep zero-limit cursor and all validation/required-native behavior unchanged. No callback effects occur in this primitive-only owner.

Pass 2: in the existing JNI bounded window, load primitive row regions only after a chunk contains a selected state. Preserve state scan order, resumable cursor, duplicate signed row IDs, output guards, fixed stack bounds and JNI exception checks. No additional state owner or allocation. Compare exact Java oracle and original native implementation.

Canonical transformation: existing Maven/OpenRewrite hash-pinned Java and text recipes in Synexia; target changes are SDK output only. Seal before/after, require fixed point and drift refusal. Recipe/template corrections precede replay. Ordered gates: patch, lint, compile, test, runtime; record failures and missing full reactor/JDK platform gates separately. Source and receiver draft PRs remain reviewable without promotion.

Catalogue reuses existing first-party findings and LeetCode Remove Element, HackerRank Java Comparator and GeeksforGeeks Remove Occurrences category references; no challenge code or new third-party body copied. This is a bounded file pass, not deep research.
