# Synexia A3 recipe receiver

M3JDK21 is not the canonical owner of reusable atomize/patternize recipes.

Canonical owner:

- repository: `hsoliwal/com.synexia`
- pinned commit: `4172b7ea5bb35f74ee19c578bddfb582eb210b5d`
- artifact: `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT`
- canonical export manifest Git blob: `42976645dab8b578ead93fc1a2be0a9f007b3dcd`
- canonical convergence source Git blob: `b11aff3ace8e77684cfa8cc13fbbb445a05c8a09`

The seven duplicate `com.synexia.rewrite.atom.*` source files formerly retained by this JDK fork are
removed. A3 loads the canonical artifact and verifies its embedded export manifest before applying
any candidate transformation.

The older `com.m3.rewrite.atom.*` package remains temporarily as a compatibility/proof-history
lane for existing M3JDK21 receipts. It is not the A3 recipe authority and must not receive new
reusable transformation logic. New reusable atomizer/patternizer work belongs in Synexia first.

OpenJDK product source remains outside Maven. A3 may write only candidate/evidence output under
`m3/build/**`; product acceptance remains `configure -> make -> jtreg -> runtime`.
