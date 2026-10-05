#!/usr/bin/env python3
"""Native evidence profile for the existing Synexia bundle owner."""
import hashlib
from pathlib import Path

import package_evidence as owner

OWNER_SHA256 = "89d43693f508c723e431296cc06f611c8de4f94df8eaddea95a2c7742feb0574"
if hashlib.sha256(Path(owner.__file__).read_bytes()).hexdigest() != OWNER_SHA256:
    raise SystemExit("EVIDENCE_PACKAGE_OWNER_DRIFT")
owner.PART_BYTES = 2 * 1024 * 1024
owner.GROUP_BYTES = 64 * 1024 * 1024
prior_make_parts = owner.make_parts


def make_parts(members):
    payload = owner.tar_gzip(members)
    if len(payload) <= owner.PART_BYTES:
        return [(payload, len(members))]
    return prior_make_parts(members)


owner.make_parts = make_parts

if __name__ == "__main__":
    owner.main()
