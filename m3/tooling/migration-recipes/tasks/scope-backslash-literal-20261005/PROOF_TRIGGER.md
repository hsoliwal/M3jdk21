# Scope backslash literal proof trigger

This evidence-only file exists to trigger the `m3/**` push verification workflow.

Behavioral source under proof is identical to PR #131 at its parent commit:
`428eafa028bc13b76385f1d0869d21adcb98bf83`.

Required gate: `mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml clean verify`.
