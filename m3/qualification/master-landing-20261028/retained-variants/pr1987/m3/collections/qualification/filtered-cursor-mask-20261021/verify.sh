#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
# Original eight-row history stays immutable; the verified source successors
# are exact first-party Java/JNI owner snapshots, NOT recipe execution proof.
set -euo pipefail
export LC_ALL=C
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
python3 "$root/m3/collections/qualification/filtered-cursor-mask-20261021/test_cursor_successors.py" -v
exec python3 "$root/m3/collections/qualification/filtered-cursor-mask-20261021/verify_cursor_successors.py" --root "$root"
