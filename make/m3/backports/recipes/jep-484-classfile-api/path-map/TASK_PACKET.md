# JEP 484 Class-File API — exact path/content mapping pass

Status: evidence-only second pass; no Java SE API materialization or promotion authority.

## Exact base

M3JDK21 master: `87590cb96fb0e2dc0f88fae8e01957f7179cd255`.

Existing JEP484 inventory pins:

- JDK21 internal baseline: `890adb6410dab4606a4f26a942aed02fb2f55387`
- JEP457 transition: `2b00ac0d02a110326846c75ea7ea535dccbb1924`
- JEP466 transition: `19a99d023e32fa9f4d26b76bd36993719e1dfe21`
- JEP484 finalization: `84ffb64cd73f8af11cf3670c6f19d282c2ac6961`
- JDK24 composition donor: `jdk-24+36`

## Goal

Materialize the next required proof from the merged inventory packet:

1. enumerate every Java21 internal classfile Java path;
2. enumerate JDK24 final public `java.lang.classfile` and retained internal classfile paths;
3. mechanically normalize only the historical `Classfile` -> `ClassFile` spelling;
4. emit direct one-to-one descendants, unmapped sources and ambiguous candidates;
5. bind source/donor content hashes to every direct descendant;
6. separately emit post-21 version/default-semantic signals for manual adaptation review.

The mapper is evidence-only. It does not decide semantic equivalence, public API compatibility, or
source-copy admission.

## Fail-closed rules

- exact pinned refs required;
- deterministic sorted paths;
- no duplicate normalized keys;
- ambiguous mappings remain explicit;
- source/donor file bytes are SHA-256 bound;
- version/default signals are hints only;
- no mutation or promotion authority.

## Output

The tool emits:

- `JDK21_INTERNAL_TO_JDK24.tsv`
- `UNMAPPED_JDK21.tsv`
- `ADDED_JDK24.tsv`
- `VERSION_SIGNALS.tsv`
- `summary.json`

This pass must complete before any source-sealed JEP484 API crate generation.
