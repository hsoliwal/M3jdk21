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

## Recipe catalogue

    mvn -B -ntp exec:java \
      -Dexec.mainClass=com.m3.a3.A3 \
      -Dexec.args="cat --root ../../.. --out m3/build/a3/catalogue.tsv"

A3Cat freezes the executable M3 recipe DAG and official OpenRewrite recipe/donor evidence before
source transformation. Catalogue membership never implies activation. Source-available donors are
mechanically restricted to REFERENCE_ONLY.

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
