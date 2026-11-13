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

## Mastery gate

A3 candidate application is not permitted from a recipe artifact alone.

Every `apply` run must provide both:

```text
--mastery-receipt <M3_RECIPE_MASTERY_FANIN_V6.tsv>
--mastery-root <expected SHA-256 root>
```

The receipt is validated by the portable verifier exported from canonical Synexia. The verified
root is copied into every A3 candidate receipt. This binds candidate generation to the compiler/JUnit,
counterexample, regex/oracle, challenge-donor, and optional JNI fan-in already completed by Synexia.

A3 does not recreate those mastery systems and the receipt grants no product-source mutation or
promotion authority.
