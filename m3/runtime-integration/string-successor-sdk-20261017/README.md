# Exact String successor SDK receiving copy

Canonical source: [com.synexia PR11973](https://github.com/hsoliwal/com.synexia/pull/11973), commit `803c41a722967a2e8fd7850aa88bf19cad887b21`. The `canonical` directory is the exact 54-file Maven module Git tree `9e0b1a456a21cceedd4dc95eb08d07aad8ea25d3`; Synexia retains recipe ownership.

This PR qualifies the original source-sealed OpenRewrite provider and serial V3→V4 tooling repair. The existing related precompute job runs Maven and retains exact XML, generated V3/V4 files and Python logs. The new fixture includes original PR11960 SDK tests and requires complete 26-test successor and 12-test compatibility spectra. The old V3 baseline must reproduce one failure and two errors.

Qualification is initially NOT_RUN. The target checker and runtime are unapplied; public Java/JNI APIs and phase state are unchanged. Once the exact SDK succeeds, apply its V3 then V4 outputs as separate serial commits and check whole phase custody independently. Full matched-JDK output remains NOT_ADMITTED: the small SDK reserve check is not a full-build allowance.

The only new public adapter is directly required generic Apache-2.0 glue; no unrelated GPU resources, private runtime code or new executor are included. Original GPL/ClassPath-exception Java source is read-only fixture data with notices and license texts preserved. See DISCLOSURE_PREFLIGHT.json and THIRD_PARTY_NOTICES.md.
