# m3/tooling/companion: companion admission planner (Java 21, no dependencies)

`jdk.m3.tooling.companion` helps decide whether an exported Java API type may get an optional M3 companion class. It
never changes the JDK class, its API, native symbols, serialization, module exports or storage.

| Class | Role |
|---|---|
| `M3CompanionPlan` | Checks requested `(module, binaryName)` pairs against the running boot layer without initialising classes. Refuses absent, non-public and unexported types. Reports installed but unresolved system modules as `HOLD_MODULE_NOT_RESOLVED`. Derives the deterministic candidate `jdk.m3.companion.<package>.M3<Type>Ops` and rejects collisions with reserved names. A `REVIEW` row is a candidate, never an admitted API. |
| `M3SystemApiCensus` | Read-only inventory of class resources in the installed system modules (`ModuleFinder.ofSystem()`). |
| `M3ClassfileSeal` | SHA-256 seal of a class file read from the runtime image. |

Run `bash verify.sh` (javac `-Xlint:all -Werror`, then the four `main` proofs). From the command line:

```sh
java -cp <classes> jdk.m3.tooling.companion.M3CompanionPlan java.base:java.lang.String java.base:java.util.List
```

Received from com.synexia `synexia-openrewrite-recipes/crates/m3jdk21-companion-admission-20261029`; see
`m3/qualification/receive-m3-tooling-companion-20261108/`. The crate's OpenRewrite discovery recipe
(`M3CompanionInventoryRecipe`) stays in com.synexia because it needs `org.openrewrite`.
