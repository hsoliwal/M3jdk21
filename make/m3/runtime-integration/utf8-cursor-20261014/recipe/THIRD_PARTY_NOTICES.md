<!-- SPDX-License-Identifier: Apache-2.0 -->
# Attribution

Apache-2.0 tooling reuses the existing public M3JDK vendored Synexia provider and
its two public support classes unchanged. The original Synexia UTF8 SDK test,
descriptor and manifest are unchanged. Their exact repository, revision, source
path, license and Git blob are recorded in sdk/pins.json and the provenance
ledger. The POM is adapted from the existing public receiving qualification POM
and carries a dated change notice.

M3String before/after fixtures retain their original GPLv2 with Classpath
Exception headers. sdk/licenses/OpenJDK-LICENSE and
sdk/licenses/OpenJDK-ADDITIONAL_LICENSE_INFO retain the donor licensing files.
These fixtures are not relicensed Apache-only. No private runtime reference
implementation or unrelated material is exported for dependency downloads.

The report-checker is only new evidence glue; the reusable recipe engine remains
unchanged. Tool-only metadata hashing uses the previously qualified MIT
emn178/js-sha256@dbb7a10027ee30304116a9ff8d8b810e0d1e6c7f without shipping it.
No challenge/editorial solution code from LeetCode, HackerRank or GeeksforGeeks
is copied; this task reuses existing recipe and qualification mechanisms.
