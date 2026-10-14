<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 optional ChromeLLM and TornadoVM Java 21 JMODs

## Admission boundary

Two new independently licensed Apache-2.0 Java modules have been received
**outside OpenJDK `src/`** at `m3/optional-jmods/modules/`. The upstream
`java.*` and `jdk.*` contracts, HotSpot, java.base and its build remain
unchanged. This is an optional SDK JMOD/JLINK receipt, **not** a certified
replacement JDK API or a passed full OpenJDK build. It does not advance the
canonical String -> arrays -> collections -> AST/compiler -> remaining
families production admission order.

Synexia owns the canonical source, OpenRewrite recipe and provenance;
M3JDK21 owns optional product assembly and runtime acceptance. The exact
source blobs in `SOURCE_LOCK.tsv` match the canonical Synexia module
implementations. ChromeLLM and the TornadoVM fork retain their respective
upstream owners and licenses. Only first-party separable source is included;
no Chrome DLLs/models, TornadoVM binaries, native libraries, model weights,
third-party Java libraries or Synexia runtime dependencies are embedded.

## Structured public API

| Module | Type | Java 21 contract |
| --- | --- | --- |
| `com.synexia.m3.chrome.chat` | `com.synexia.m3.chrome.chat.ChromeChat` | Loopback-only `/v1/chat/completions` requests; `Message` and `Completion` records; UTF-8 JSON/non-streaming only; bounded messages/HTTP replies |
| `com.synexia.m3.compute.vector` | `com.synexia.m3.compute.vector.VectorCompute` | CPU-correct integer vector addition; immutable `Result`; `Provider` ServiceLoader SPI; exact independent CPU comparison and no implicit GPU acceptance |

The Chrome client calls a **separately operated** local Chrome2api-compatible
server. It neither discovers Chrome profiles nor starts a process; the caller
must provide and license the local runtime/model. `ChromeChat` accepts only
an explicit TCP port for literal `127.0.0.1`; it follows no redirects,
uses no proxy, and rejects non-200/unparseable/oversized responses.

The vector facade is CPU-first. A hardware provider must be separately
packaged with the matching API and dependencies; `addWithBackend` uses an
exact provider ID, never an arbitrary Java class name. The provider must
return Java `int` wraparound addition with exact length/value parity and
must not mutate input copies. If it fails, the call fails closed. No
GPU/device success is asserted by module-only smoke tests.

## Reproduce the optional JMODs with JDK 21

From the M3JDK21 checkout root (no WSL needed):

```sh
java --source 21 m3/optional-jmods/tools/M3JmodBuild.java verify m3/optional-jmods
```

This checked-in build tool requires Java 21, compares the declared
`tool-count.txt` and module directories, compiles strict
`javac --release 21 -Xlint:all,-module -Werror`, invokes both module
self-tests, runs `jmod create --date`, validates `jmod describe`,
repackages each JMOD and refuses non-repeatable bytes. It produces
`m3/optional-jmods/target/jmods/*.jmod` and `target/JMOD_BUILD.tsv`.
Generated JMODs and images must **not** be checked into Git.

An optional jlink assembly on Linux/macOS, assuming matching JDK 21 tools:

```sh
jlink --module-path "$JAVA_HOME/jmods:m3/optional-jmods/target/jmods" \
  --add-modules java.se,com.synexia.m3.chrome.chat,com.synexia.m3.compute.vector \
  --output m3/optional-jmods/target/image
m3/optional-jmods/target/image/bin/java --list-modules
```

On Windows use `%JAVA_HOME%\jmods;m3\optional-jmods\target\jmods` as
the module-path and `target\image\bin\java.exe` as the launcher.
These are **optional assembly commands**, not proof that the whole modified
OpenJDK tree has been configured and built with the modules in `src/`.

## CPU and HTTP contract usage

```java
import com.synexia.m3.chrome.chat.ChromeChat;
import com.synexia.m3.compute.vector.VectorCompute;
import java.util.List;

var sum = VectorCompute.add(new int[] {1, 2}, new int[] {3, 4});
int[] output = sum.values(); // [4, 6]
try (ChromeChat client = new ChromeChat(11435)) {
    var reply = client.complete("chrome-gemini-nano",
        List.of(new ChromeChat.Message("user", "Hi")), 128);
    System.out.println(reply.content());
}
```

The second operation works only with a currently running, caller-authorized
local Chrome2api-compatible runtime.

## OpenRewrite route and evidence

The canonical Synexia class-backed engine is
`M3HashPinnedJavaSnapshotRecipe`, activated with the
`m3-chrome-tornado-jmods-20261014` resource crate.
`manifest.tsv` binds each target path to an expected
`ABSENT -> SHA-256` Java source snapshot. Replay must be fixed-point and
refuse source drift; the recipe is a **candidate generator**, not a JDK
promotion authority. The target source is committed here with matching
source hashes, but OpenRewrite replay against this checkout and Maven/reactor
qualification have **not** been executed.

Local external Java 21 evidence for this source snapshot:
module self-tests passed; `javac --release 21` strict passed;
two JMODs created/described; one `jlink` image resolved both;
a localhost simulated server and vector contract harness passed 18 checks.
The test executable is `tests/BridgeIntegrationTest.java`.

Pending: full M3JDK21 configure/make/SDK image, jtreg,
native/JNI platform gates where applicable, complete 20-module donor
builder verification, production ChromeML model-backed inference,
physical TornadoVM acceleration, numerical/device parity and benchmark evidence.
Do not publish an unqualified throughput or release claim.

## Source and licensing

- Canonical: `hsoliwal/com.synexia/synexia-m3-jmods` and its OpenRewrite recipe resources.
- Donors: `hsoliwal/ChromeLLM` and `hsoliwal/synexia-tornadovm`.
- Source map: `SOURCE_LOCK.tsv`; exact Git blob hashes are verified before promotion.
- Target license: separately licensed Apache-2.0 modules under `m3/`; OpenJDK code remains under its original license.
