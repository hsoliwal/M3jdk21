# Current Ic0 task and evidence checkpoint

This draft delivers the isolated `m3-current-integration-c0` recipe task and its fresh proof package. Active source and M3 owners remain unchanged. The authoring epoch is source `d1ecbd43dbadaf218deec0d98a2ddc23f3a3f45c` and M3 `c0a14387009aefc7d62bd3268055d526e04f9074`.

The actual fresh producer passed all four ordered gates and 42 JUnit methods with no failures, errors or skips. It produced 143 exact outputs in 36 families (69 replacements and 74 additions). Those outputs are retained as task/proof evidence; this checkpoint does not install them into active canonical paths.

The actual archive package, verification, extraction and full readback all passed. [READBACK.json](READBACK.json) records 2,873 exact logical bodies. [PROJECT-READY.json](PROJECT-READY.json) identifies the complete 292-file qualified Maven project. The flat review mirror has 290 qualified project files: two before-image fixtures contain significant trailing TABs and are deliberately archive-only. The flat mirror is not Maven-ready. Follow the checked [reconstruction entry](reconstruct.py) and [instructions](README.md) to verify and extract the complete project into a fresh directory before invoking Maven. The original POM, fixture bytes and receipt paths remain unchanged.

This is a source and proof package, not a hermetic tool image. The unchanged archive owner excludes compiled artifacts, toolchains, dependency caches and settings. Of 2,643 sealed input references, 1,059 bodies are included and 1,584 are identified explicitly by identity only. A subsequent Maven invocation is a new execution and does not become an original proof receipt.

No receiving, latest-head integration, full JDK/runtime, performance or hosted-workflow success is claimed by this checkpoint. Any separate receiving result must retain its own actual status and scope. The source/runtime producer PASS and archive PASS are the admitted evidence for this task-only draft.

[HISTORY-AVAILABILITY.json](HISTORY-AVAILABILITY.json) preserves the earlier workspace census: 224 of 591 historical mapped bodies were unavailable in that census. It does not assert remote absence, and later recovery does not reconstruct lost proof runs. The new package has its own complete finite selection; it does not claim the older delivery was complete.

[CURRENT-DELIVERY.json](CURRENT-DELIVERY.json) binds the exact receipt and package identities. Original absolute paths in receipts are custody identifiers, not download links. Use [SOURCE-CLOSURE.json](SOURCE-CLOSURE.json) and the archive logical paths after reconstruction. Final per-repository manifests include this explanatory envelope separately from the qualified project inputs.

One static wiring finding remains explicit: the TQ-sync default verifier requires the historical 24-file sealed after-state, including a 52-row mapping, while the fresh task generates an 87-row mapping preserving those 52 records plus 35 pending records. A separate current-context profile is being prepared. This is a source inspection, not an executed workflow failure. No hosted-workflow or supplemental-verifier success is claimed.

An earlier alleged JEP485 catalogue mismatch was withdrawn: its workflow reads `m3/backports/JEP_CATALOGUE.tsv`, whose relevant value is `candidate`; the `CANDIDATE_UNVERIFIED`/`PENDING_CI` fields belong to a different catalogue. No JEP485 blocker was established by that comparison.

The physical selection contains 338 files per repository: the original 318 task/package rows, 17 execution/review rows, and these three explanatory files. [PAIRED-FILES.json](PAIRED-FILES.json) intentionally identifies the initial 318-row cohort. `CURRENT-DELIVERY.json` enumerates the 17 additional rows and the three-envelope boundary; the final external publication manifest binds all 338 exact physical identities without a self-hash cycle.
