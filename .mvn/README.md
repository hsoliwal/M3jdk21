<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Shared Maven recipe bootstrap root

This directory marks the repository's Maven root. Maven 3.9.9 finds it when launched from a nested project or with `-f` and sets `maven.multiModuleProjectDirectory` before profile activation. The installed boot profiles use the shared `.m3/recipe-boot.properties` configuration at this root.

The canonical Synexia recipe `com.synexia.m3.InstallRecipeBoot20261021` installs the pinned authoring tool `com.synexia.m3:m3-pr-stack-preservation:1.0.0-m3boot20261021` through `exec-maven-plugin:3.5.0`. It remains build tooling and does not become a Java/HotSpot/JNI runtime dependency. Install the fixed tool artifact before enabling the configuration.

At the pinned installation baseline `3055b76e294423da047c0a2ee030045bca97ef00`, all 635 Maven POMs are inventoried: 487 eligible entry points are covered by 457 direct profile installations and 30 inherited child models. The remaining 148 POMs are exact upstream, custody, resource-fixture or historical proof inputs whose bytes stay sealed. Every selected active Maven POM is covered by its own independent parent root or an inherited profile. Maven aggregation alone does not create parent inheritance. Existing local parent relationships are reused; the coverage receipt identifies the independent roots and excluded custody, upstream-reference, resource-fixture, and historical proof POMs.

The automatic `validate` hook checks pinned PR metadata and patch envelopes; Git applicability is checked by the explicit apply/build runner. Its three local arguments pass the shared configuration, current project directory, and `${session.projects[0].basedir.absolutePath}` to the runner, so only the first project in the selected Maven session performs the check. The hook does not set JVM system properties. Standalone nested invocations and `-pl`/`-rf` selections use the same shared configuration. The absolute-path accessor is required because Maven's String argument converter does not accept the File-valued expression alone.

Keep the shared configuration in `mode=check` for lifecycle use. Run the explicit bootstrap CLI before Maven for `apply` or `build`; those operations create an isolated candidate and require a fresh Maven session after source changes. A candidate build supplies `-Dm3.recipe.boot.skip=true` to avoid recursion. The same property is an explicit caller bypass of automatic lifecycle checks.

For a strict check before any project begins in a parallel `-T` reactor, invoke the bootstrap CLI before Maven. A lifecycle hook in the first selected project cannot impose a global startup barrier on concurrently scheduled independent modules. The hook checks configured, pinned PRs; it does not select floating updates or treat an AI suggestion as patch authority.

Canonical source, generated POM before/after hashes, per-POM coverage, actual OpenRewrite fixed-point/refusal results, and the real Maven lifecycle argument fixtures are retained in Synexia's `m3-recipe-boot-20261021` task crate. Product acceptance and broader JDK/runtime qualification remain separately scoped.
