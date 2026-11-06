<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Exact parse-context ordering refusal

The initial unpromoted source-custody generator listed engine parse contexts in declaration order. The unchanged canonical Java snapshot engine requires lexical path order and correctly refused the sealed crate with `invalid hash-pinned Java parse context row`. This folder retains its exact manifest, templates, contexts and actual failure report. The successor sorts the context paths before sealing and is separately SDK-qualified; no engine, test or historic seal changes.
