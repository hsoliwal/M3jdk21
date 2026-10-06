# A3 tooling

A3 = Atomize -> Patternize -> Absorb.

This Maven module is a JDK authoring/proof tool. It is not part of the JDK runtime and does not replace the OpenJDK configure/make build.

## Verify

    mvn -B -ntp -f m3/pom.xml -pl tooling/a3 -am clean verify

## Inventory

From this module directory:

    mvn -B -ntp exec:java \
      -Dexec.mainClass=com.m3.a3.A3 \
      -Dexec.args="inv --root ../../.. --out m3/build/a3/inventory.tsv"

## Plan

    mvn -B -ntp exec:java \
      -Dexec.mainClass=com.m3.a3.A3 \
      -Dexec.args="plan --root ../../.. --out m3/build/a3/plan.tsv"

## Apply an explicit FILE candidate

    mvn -B -ntp exec:java \
      -Dexec.mainClass=com.m3.a3.A3 \
      -Dexec.args="apply --root ../../.. --out m3/build/a3/run-001 --file src/java.base/share/classes/.../Target.java"

A3Apply writes only under m3/build. The original src/test file remains unchanged.

Candidate output:

    m3/build/a3/run-001/candidate/<original path>

Receipt:

    m3/build/a3/run-001/receipt.tsv

The receipt records FILE scope, before/after SHA-256, changed state and fixed-point proof.

See ../../docs/a3.md for the whole-JDK absorption contract.

## Synexia convergence borrow

A3 now consumes an exact Apache-2.0 Synexia convergence snapshot pinned by
`m3/tooling/a3/synexia/SOURCE_MANIFEST.tsv`.

`A3 synexia` verifies the borrowed public-polish, donor, atomizer, patternizer, regex-mastery
and recipe-mastery source blobs. The snapshot is tool-plane input only; it grants no OpenJDK
product mutation or promotion authority.

`A3 polish` adapts Synexia's all-project public-code polish model to the real OpenJDK
`src/` and `test/` inventory. Every Java-bearing target is routed to
`com.m3.a3.SynexiaPublicPolish`; targets containing native code carry an additional native/JNI
gate. Pure native/resource/build targets remain verification-only.

The OpenJDK `configure -> make -> jtreg -> runtime` chain remains the product oracle.
