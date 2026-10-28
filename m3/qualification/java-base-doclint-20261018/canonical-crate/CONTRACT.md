<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Sealed source-only contract

The authorized edit floor is the `java.base` module because the exact 12 owners span packages. The recipe applies serial file replacements and changes only comments. Public declarations, executable bodies, literals, exception behavior, monitor ownership and cancellation order remain unchanged. Exact owner paths and before/after SHA-256 values are recorded in the resource manifest.

The reusable snapshot engine admits the complete source set before publishing any replacement. Inputs may be exact baseline or exact admitted output, allowing idempotent and partial replay. Missing, duplicate, drifted and late-changing target sources refuse. Corrupt resource templates refuse. Unrelated Java units remain untouched.

The proof requires actual SDK outputs and the pinned compiler context. Documentation repairs are checked by comment-free Java syntax identity and serial diagnostic accounting. Scope annotations are checked by exact line removal, SOURCE-retention evidence, strict zero-warning compilation and fifteen classfile/API comparisons. A complete runtime build is an independent gate in the receiving PR and must be reported from an actual image run.
