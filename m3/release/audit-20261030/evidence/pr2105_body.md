## Problem and resulting behavior

The existing desktop build workflow references retired macOS/Windows hosts, uses Intel-only bootstrap assumptions for Apple Silicon, and deletes build archives after test cleanup. This change makes native desktop build jobs usable on the selected hosted images and adds an explicit way to retain identified development previews.

## Changes

- Build and test macOS x64 on `macos-15-intel` and ARM64 natively on `macos-15`, using Xcode 16.4 and architecture-matched boot JDKs. Add the ARM64 lane to the existing ten-part tier1 workflow.
- Move Windows jobs to `windows-2022`; select and verify one VS2022 installation and the exact 14.29 MSVC architecture component before configure.
- Pass the selected boot JDK to JTReg and isolate its cache by host architecture.
- Update checkout/cache/github-script runtime references in the existing main workflow call graph.
- Add SHA256SUMS and repository/revision/run/platform metadata to uploaded archives.
- A manual `keep-bundles=true` run retains previews for 14 days and suppresses final cleanup. Ordinary runs preserve existing cleanup behavior.
- Add a guide that distinguishes native builds, cross builds, incomplete lanes and Android/iOS porting requirements.

## Canonical recipe custody

Source PR: https://github.com/hsoliwal/com.synexia/pull/13969
Source commit: `d82e234e84dccb11b2d5b26e15217850387c4473`
Canonical crate: `synexia-openrewrite-recipes/recipe-crates/m3-platform-preview-20261029`
Product preimage: `669a949d192345acefcfc5e9bb0b3724174d270c`
Actual SDK output receipt SHA256: `488d86cfac5fef554c89a9f8584c3b22bc4023cb6939bc0c0033e9c2cacb1530`

All 13 files are exact actual OpenRewrite SDK results. The source PR owns the recipes, frozen before/after fixtures, correction history, donor catalogue and execution proof. GitHub readback verified every receiving blob; no recipe runner is copied into this product.

## Checks completed

- Final OpenRewrite replay: **9 tests passed**, zero failures/errors/skips, including exact replay/fixed point, reversed and mixed inputs, refusal and no-partial-publication checks.
- Independent workflow checks: **12 passed**, including executed archive hashing and JTReg selection Bash snippets and 20 retention cases.
- actionlint **1.7.12 passed** on all six changed workflow files; ShellCheck/Pyflakes were unavailable and explicitly disabled.
- Independent read-only review verified all 13 received files equal the SDK output and found no blocking packaging/control-flow issue.
- Existing build/debug matrices, ten tier1 partitions, configure/build targets and failure reporting are preserved.

## Required execution before availability claims

Draft: **native hosted builds, Windows PowerShell installation, full target tier1 and release gates have not been executed by these local proofs**. A successful manual preview run is still needed before advertising downloadable builds. Windows ARM64 remains HotSpot-only; Linux cross compilation does not prove target execution.

Android and iOS remain explicit runtime porting targets. An Android AAR does not qualify the full standalone Java21 VM. OpenJDK Mobile's static Zero iOS implementation is a donor direction requiring Java21 reconciliation, native dependency review, signing, and separate device/simulator tests.

No all-platform readiness, compatibility certification or performance improvement is claimed by this build-infrastructure PR.

## Exact receiving-file receipt

Recipe ID: `com.synexia.m3jdk.PlatformPreview20261029`. Columns: target path, preimage SHA256, actual SDK postimage SHA256.

```tsv
# OpenRewrite SDK Results; no template-copy materialization
.github/actions/do-build/action.yml	cbeeca33d23c2d19423d44e83385b0eaa08e38df634ebe03944a65214ec94789	12cb6188a4eaa98345c54e63bfbda5d48704c6be439b417e8343d8d500040529
.github/actions/get-bootjdk/action.yml	1c1b50b7cd70d7217f271320a954649ca7ea65855df6d227eb7607d5a5a25f1c	2b480605694752c177c1cd7ece4a2469415b01a4bdc8ff1f8c011d8a6f73f1b7
.github/actions/get-gtest/action.yml	4f1b6d635bd1c25d069e39f249cd22162b157c140e210ca817fab7699a2c9000	93d423c34f0f7c2f462fe455fa5fffcbf58a37ae99a8396a5a64c29982e6a30b
.github/actions/get-jtreg/action.yml	6f6d9096b4640f210aa89f7bc4b05bbc8e8de5869383de67a36eb915a4b5ebd0	a098b7869d54cefff0b27e837b1a0a2b3c21d4eba9c2a8117dda6a74a48bcde9
.github/actions/upload-bundles/action.yml	c1aac32bc122887d5f7f90847887524f9ab874bcd072846e1f86c880a35df604	ef940da2cccd1c0f6a0e8013df72b11692a67009096527ab806c39635a61a565
.github/workflows/build-cross-compile.yml	81f15e88efb83c0967f14ee90d82ff26019ef7d28dd3f1804792b7707dbf0adb	497900526a2c5874704fa2971b0b98d3dd5effbfa6518c92e30c1d8d49f8c7e9
.github/workflows/build-linux.yml	167f03e62609359ed9d56bb66efeeea832ec2eada02c5ae1577854cc9c6dd7cc	10e47434b5e956bfd39b1334066751412e17109cee33a39c1d397d83b5187368
.github/workflows/build-macos.yml	add7664ffa0aa71365719217b207854cb33485db266130cd1ff7640c36250eba	ccd30866e1957f688662500ca2d30b35f41b1c450e542c38595cbbd4b79542a3
.github/workflows/build-windows.yml	d72dc367d1cba62716475cca62a1b19c7f884c98040d160bf0c88ffff5a1dc3c	010fcb89feae86d90ee01017fb4a3ef02be7b0e70ddde47a0205c9cb54044874
.github/workflows/main.yml	077630022be354c322f5c0be3d3f18b2f0d61f15f045195402803d220d34e4a7	57423875905b05869c462fbf4b72b2ac765e56a9db525b512db96f9afa74ec8d
.github/workflows/test.yml	e4401b7bdeffbda21fdc0eacec62644350c3593189767461c063bd2bb802fa06	ff4c3ca9cc9937abfbb41e478ea8defbfa5a62614414c7386b8de94824b0e72d
m3/release/platform-build-previews.md	ABSENT	a26aa6c08c024aff80eb3dd7d21f60a2357875be1060b122a4a383b1396423ca
make/conf/github-actions.conf	09c15edec773bde6cea66d293b6717b826da2a1bca1328d045b541d3acaaff4a	9ef8526adb85e2e477d0e8eab0870b890b76af7bcff7fd4d4b7a39f0e1d3082e
```

## Hosted status at 2026-10-29 10:34 UTC

All 14 checks were queued. The [native build run](https://github.com/hsoliwal/M3jdk21/actions/runs/40853790908) has only the queued Select platforms job, with runner ID 0 and zero steps. No native platform job has executed and no artifacts are available.

The workflow supports manual `keep-bundles=true` retention, but that manual request has not been dispatched. The automatic push run preserves ordinary cleanup behavior. Download availability still requires an actual successful retained-preview run and applicable target qualification.

