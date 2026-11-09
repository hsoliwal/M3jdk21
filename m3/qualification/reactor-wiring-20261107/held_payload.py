#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Print (bytes, no newline translation) the recorded HELD block of one module from EDITS.tsv, for unhold.sh.

The payload column is a JSON string; the row is the last one whose note starts with "<module> HELD".
CLI: python -I held_payload.py EDITS.tsv <module>
"""
import json
import sys

rows = [line.rstrip("\r\n").split("\t") for line in open(sys.argv[1], encoding="utf-8")][1:]
hits = [r for r in rows if r[7].startswith(sys.argv[2] + " HELD")]
if hits:
    sys.stdout.buffer.write(json.loads(hits[-1][6]).encode("utf-8"))
