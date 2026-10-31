# OpenJDK 21 verbatim baseline oracle

M3JDK21 pins its source oracle to OpenJDK 21 GA commit:

`890adb6410dab4606a4f26a942aed02fb2f55387`

Both upstream tags `jdk-21+35` and `jdk-21-ga` resolve to this commit.

The oracle is deliberately simpler and stricter than semantic equivalence. For every baseline path it records the exact byte SHA-256 and verifies exact byte equality. Each path is classified as:

- `VERBATIM_EQUAL`
- `MODIFIED`
- `ADDED`
- `DELETED`

The M3-owned `m3/` tree and Git metadata are outside the OpenJDK source oracle.

This evidence is the first plane used when authoring/refining OpenRewrite recipes:

```text
OpenJDK21 exact baseline
        ↓
verbatim path/hash ledger
        ↓
changed-file frontier
        ↓
semantic/contract hash
        ↓
OpenRewrite recipe
        ↓
JUnit recipe proof
        ↓
dry-run candidate diff
        ↓
compile/test/runtime proof
```

A semantic hash never replaces the verbatim ledger, and a verbatim difference never implies semantic drift. They are separate evidence planes.
