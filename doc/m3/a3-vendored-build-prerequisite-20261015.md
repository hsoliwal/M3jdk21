# M3 A3 source-vendored Maven dependency bootstrap

Status: CI prerequisite repair; no OpenJDK runtime/source semantics change.

## Observed failure

On M3JDK21 PR #904, the `M3 Synexia full delivery A3` workflow's
`Recipe replay and fixed point` step failed resolving:

- `org.openrewrite:rewrite-java:8.17.1-m3-source-1`;
- `org.openrewrite:rewrite-java-21:8.17.1-m3-source-1`;
- `org.openrewrite:rewrite-test:8.17.1-m3-source-1`;
- `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1`.

These are intentionally private, source-derived coordinates, not Maven Central artifacts.
The exact upstream source and provenance already belong to the existing
`m3/vendor/third_party` Maven reactor. Replacing the coordinate suffix with public
binary dependencies would violate the repository's source-vendoring custody model.

## Deterministic prerequisite

Build the minimal Maven dependency closure from checked-in source, before invoking the
downstream A3 recipe test:

```bash
mvn -B -ntp -f m3/vendor/third_party/pom.xml \
  -pl rewrite-java,rewrite-java-21,rewrite-test,junit-jupiter-5.10.2 \
  -am -DskipTests install
```

Maven `-am` builds declared source-module prerequisites in reactor order; it does
not grant any Java 21 backport, recipe promotion, or compatibility authority.

## Proof boundaries

The A3 `SourceFile` type fix is separately owned by canonical Synexia PR #11460,
received in M3JDK21 PR #903. Historical scope custody is owned by Synexia PR #11472
and received by M3JDK21 PR #904.

This packet adjusts only **workflow orchestration**, reuses the existing Maven
source reactor, and preserves all existing A3 JUnit, OpenRewrite, Maven, diff,
fixed-point and full reactor checks. CI must show actual successful builds before
the prerequisite can be treated as qualified.

The full backport catalogue/JEP build, jtreg, JNI/HotSpot and release proof remain
separate, unwaived gates.
