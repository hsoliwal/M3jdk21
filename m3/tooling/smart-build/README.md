<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 SmartBuild receiver (opt-in)

This parentless Java 21 Maven project is a **thin receiver**, not an alternative build cache,
compiler, reactor planner, Git scanner, or JNI model implementation. Original canonical
sources and source-sealed recipe: `hsoliwal/com.synexia`, PR #14672,
`synexia-build-inventory` and `synexia-m3-recipe/task-crates/m3-smart-build-20261031`.
The existing `com.m3.file.M3FileDelta`, M3AST/javac facts, and action-cache candidate
retain their own ownership. M3JDK21 `java.*`, JNI and HotSpot public contracts are unchanged.

## Build and invoke

```sh
mvn -B -ntp -f m3/tooling/smart-build/pom.xml verify
java -jar m3/tooling/smart-build/target/m3-smart-build-receiver-0.1.0-SNAPSHOT.jar \
  --jdk-root /work/M3JDK21 \
  --smartbuild-jar /approved/synexia-build-inventory.jar \
  --jar-sha256 <approved-jar-sha256> \
  --baseline <exact-git-sha> \
  --project m3/file --maven-executable mvn \
  --evidence /work/receipts/m3-file --run
```

The approved SmartBuild JAR must be independently built from the reviewable Synexia source
and pinned by its exact SHA-256. A Git checkout alone does not qualify it. No implicit remote
fetch, model startup, or shell command execution occurs. `--run` is optional: without it the
canonical planner only creates the plan and receipts. For changed files outside the chosen
`m3/` Maven project, the canonical planner refuses instead of claiming a complete JDK build.

If and only if the selected active Maven reactor has been audited, pass
`--active-modules /approved/m3-reactor.txt` (one Maven-project-relative module path per line).
Missing or uncertain inventories delegate to full Maven `verify`. The standalone `m3/file`
project can be used without a module inventory. This receiver does not alter default `make`
/OpenJDK build behavior and is not added to `m3/pom.xml` by default.

Optional `--remote-cache-url https://approved.example/cache` is forwarded to the canonical
SmartBuild client; it requires the reviewed Apache Maven Build Cache Extension 1.3.0 in
that Maven project's `.mvn/extensions.xml`, and remote publishing remains disabled. Optional
`--chrome-origin http://127.0.0.1:11435` forwards only bounded diagnostic categories to the
existing authenticated browserless ChromeML Java/JNI service. AI output is untrusted advice.
Maven/Javac/JUnit/JNI/jtreg/HotSpot results remain authoritative.

The Java runtime portion has no third-party dependencies; JUnit is test-scoped only. The
receiver validates real non-symlink jar/project paths, exact hash and Git baseline, an
external evidence directory, and explicit command arguments. Full Maven, jtreg, JNI and
Windows/WSL qualification must be performed with an actual installed JDK and approved runtime.
