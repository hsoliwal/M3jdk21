<!-- SPDX-License-Identifier: Apache-2.0 -->
# Synexia direct-copy intake review ledger reconciliation

The original verifier compares every migration record marked copied_code=true
from hsoliwal/com.synexia with the independent licensing review ledger.
On the product baseline, 29 such mapping records exist but 27 reviews were
listed. The parser JMOD workflow refused before reaching Java compilation.

Append-only correction: preserve all 27 original reviews byte-for-byte,
and register m3.build.change-journal-doclint plus
synexia.regex.prepared-utf16-effects.receiver as REVIEW_REQUIRED.
Both source mappings remain pending: the ledger explicitly demands
rightsholder-approved target-compatible relicense OR target-owned
reimplementation avoiding direct incompatible Apache source commingling.

This audit is not legal clearance, product ABI admission, JVM compatibility,
a donor-source import or evidence of any recipe having executed.
All original family, receiver and intake policy gates remain enabled.
