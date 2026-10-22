# Source-only bootstrap runtime recipe attribution

The existing GEF LICENSE and NOTICE remain unchanged (EPL-2.0 root license).
OpenRewrite FindAndReplace 8.90.4 is reused from openrewrite/rewrite commit
398a6349648ca802aa050bdb6fe45a99395dce47; Apache-2.0.txt and exact donor source are retained.
The proof harness adapts the existing qualified Synexia ContainerArea official harness,
retaining Apache-2.0. JetBrains annotations 26.1.0 is a proof compile dependency under Apache-2.0.
SLF4J API 2.0.17 is a normal Maven runtime dependency under MIT; its actual embedded license
is retained in SLF4J-MIT-LICENSE.txt. No donor dependency or SDK binary is copied.
SLF4J uses its own no-provider NOP behavior; no logging provider or warning suppression was added.
