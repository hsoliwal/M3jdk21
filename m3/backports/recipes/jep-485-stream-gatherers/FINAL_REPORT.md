# JEP 485 Stream Gatherers — candidate implementation status

The current M3JDK21 tree already contained a reviewed 15-target JDK24 JEP 485 donor crate but no dedicated API-authority wrapper or executable admission packet. This change supplies that missing control plane without hand-editing `java.util.stream`.

The new `M3Jep485BackportRecipe` composes the existing hash-pinned snapshot engine over `jdk24-jep485-stream-gatherers`. `M3RecipeScopeRegistry` classifies this wrapper as `LIBRARY_API / EXPLICIT_CONTRACT_CHANGE`; the generic snapshot recipe keeps its narrower reusable policy. A named OpenRewrite recipe is provided for Maven activation.

The authored JUnit proof reads the four actual current checkout owners, requires the eleven new targets to be absent, runs the real recipe, requires fifteen exact donor postimages, rejects source drift and occupied additions, validates the final Gatherer API shape, and requires a full second-pass no-op.

No product source has been promoted by this packet yet. Recipe compilation/execution, matched `java.base` build, focused jtreg, API diff and parallel/JMM runtime behavior remain mandatory. Until those pass, JEP 485 remains a candidate rather than an admitted compatible backport.
