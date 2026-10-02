# Compiler tooling reuse

OpenRewrite 8.17.1 and the OpenRewrite Maven plugin 5.23.1 are reused as external
Apache-2.0 dependencies. No OpenRewrite source implementation is copied. Their
published dependencies retain their own licenses and notices.

The eligibility and visitor pattern were inspected in
`hsoliwal/com.synexia@6df9df8d8f42111239013ee941723ec37f97ba6e`:
`M3MIndexCanonicalFromStringRecipe` and
`M3MIndexStringConstructionCandidateRecipe`. This bounded adapter implements
the missing M3 explicit-view boundary; it does not vendor that private module
or its unrelated dependency closure. Its Apache-2.0 notice is retained.

The local OpenClaw harvest was inventoried before implementation and did not
provide a matching Java typed-lowering capability. No OpenClaw source is copied.
This Maven module is outside `java.base` and outside the explicit text runtime.
