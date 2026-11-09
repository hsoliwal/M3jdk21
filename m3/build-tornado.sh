#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
M3_TORNADO_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 "$M3_TORNADO_ROOT/m3/runtime-integration/tornado/build_runtime.py" \
  --repo-root "$M3_TORNADO_ROOT" "$@"
