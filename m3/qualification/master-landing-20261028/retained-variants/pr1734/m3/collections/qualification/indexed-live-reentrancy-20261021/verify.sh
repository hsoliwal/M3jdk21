#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
python3 "$ROOT/m3/collections/qualification/indexed-live-views-20261021/verify_recipe.py"
bash "$ROOT/m3/collections/qualification/indexed-live-views-20261021/verify.sh"
echo "M3_INDEXED_LIVE_REENTRANCY_JAVA21_DONE"
