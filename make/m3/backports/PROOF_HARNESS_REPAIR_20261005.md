# Proof harness drift repair — 2026-10-05

This packet repairs two deterministic verification-harness defects exposed by exact-head GitHub Actions.
It changes no OpenJDK product source, API, JNI ABI, recipe semantics, dependency or coverage threshold.

## JEP 467

Workflow run 37271044303 compared the pinned implementation commit
`0a58cffe88ba823e71fcdcca64b784ed04ca5398` against
`m3/backports/recipes/jep-467-markdown/PATHS.txt` and found one omitted path:

`test/langtools/tools/javac/processing/model/util/elements/TestGetDocComments.java`

The implementation denominator is therefore 251 paths, not 250. The repair adds exactly that path
and updates the workflow assertions/accounting labels from 250 to 251.

## JEP 493 / shared file-delta inventory

Workflow run 37271044265 selected only release 24 and fetched JDK 21 + JDK 24, but
`file_delta_inventory.main()` verified every configured donor tag 22..27 before honoring the
selected release. That made an otherwise valid single-release run fail on an unfetched JDK 22 tag.

The repair changes verification to require the baseline plus only the donor refs selected by
`--release`. A regression test creates a repository containing only JDK21 and JDK24 tags and runs
`main --release 24`; unused 22/23/25/26/27 refs must not be required.

## Admission

Verification remains diff -> lint/syntax -> unit tests -> workflow runtime. These repairs do not
claim that JEP 467 or JEP 493 are compatible backports; they restore the proof machinery needed to
evaluate those candidates.
