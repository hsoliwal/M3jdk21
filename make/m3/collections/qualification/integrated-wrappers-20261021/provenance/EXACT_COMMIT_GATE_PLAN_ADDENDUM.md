<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Bounded unsigned commit offset reconstruction

This addendum precedes the unsigned encoder correction. The original plan stays
unchanged and remains binding except for its unsigned `+0000`-only assumption.
The coordinator's actual-object check found that GitHub's API normalizes unsigned
commit dates to UTC and does not preserve the original Git header offsets.

The signed base `408373f4101fdca2fb3625bec8af1234e5fd3e26` supplies the observed
`+0530` offset in its exact verification payload. For unsigned documentation
commit `47238a91be57bb30b28548c2380887db8ac7ab98`, the coordinator tried only
`+0000` and that observed `+0530` independently for author and committer, with
the existing two terminal-LF message alternatives. Exactly one complete raw
commit matched the known Git SHA: both offsets `+0530`, with the unchanged API
message and no added LF. The UTC-only encoder failure is retained in
`qualification/GIT_OBJECT_ENCODING_UNSIGNED_FAILURE.json` and its source is
retained in `design/provenance/history/run_exact_commit_gate_unsigned_utc_only.py`.

The adapter may therefore construct at most eight unsigned candidates: two
author offsets, two committer offsets and two message alternatives. The epoch,
identity, tree, ordered parents and message contents remain bound to the API
metadata. Accept exactly one full-object SHA match. No other offset, timestamp,
header, encoding or message search is authorized. A missing or ambiguous match
still refuses the gate before any Git object is written. This is reconstruction
of object identity; it adds no signature-trust or product-qualification claim.

The prior signed correction also preserves every signature LF as a space-prefixed
`gpgsig` continuation, including a final empty continuation when the supplied
signature ends in LF. Removing that final LF produced a different object ID;
preserving it reproduced the exact signed base ID. Both initial failures and
their complete source revisions remain available as authoring history.
