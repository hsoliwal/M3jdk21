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
      -Dexec.args="apply --root ../../.. --out m3/build/a3/run-001 \
        --file src/java.base/share/classes/.../Target.java \
        --mastery /absolute/path/M3_RECIPE_MASTERY_FANIN_V6.tsv \
        --mastery-root <64-hex-root>"

A3Apply writes only under m3/build. The original src/test file remains unchanged.

Candidate output:

    m3/build/a3/run-001/candidate/<original path>

Receipt:

    m3/build/a3/run-001/receipt.tsv

The receipt records FILE scope, before/after SHA-256, changed state and fixed-point proof.

A3Apply also writes `mastery.tsv` beside the candidate receipt. Application is refused unless a
strict complete V6 mastery receipt and its exact root are supplied. Inventory and planning do not
require the receipt.

The checked-in mastery fixture under `src/test/resources` is TEST-ONLY. It proves the portable
parser/gate mechanics; it is not production mastery evidence. Real A3 absorption must consume the
exported Synexia mastery receipt.

See ../../docs/a3.md for the whole-JDK absorption contract.
