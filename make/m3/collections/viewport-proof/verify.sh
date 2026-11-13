#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Retain the existing entrypoint; the source-bound runner owns ordered evidence.
set -euo pipefail
exec python3 "$(cd "$(dirname "$0")" && pwd)/verify.py" "$@"
