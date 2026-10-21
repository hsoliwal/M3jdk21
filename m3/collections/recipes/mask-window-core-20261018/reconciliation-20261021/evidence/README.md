<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact execution evidence

Serial and current RAW_LOGS.json retain every original raw log as an exact UTF-8 string, keyed by the log filename referenced in COMMANDS.json. Each entry has its actual SHA-256. Materialize a log by writing its content as UTF-8 without newline normalization and compare its digest. This packaging changes no command, exit, observation or receipt. The original on-disk logs remain in the task proof directories.
