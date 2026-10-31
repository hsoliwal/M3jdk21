# Synexia A3 recipe receiver

M3JDK21 is not the canonical owner of reusable atomize/patternize recipes.

Canonical owner:

- repository: `hsoliwal/com.synexia`
- canonical merged commit: `a062661e91e769a00c2d027e1f04a7a0c8f9dc28` (Synexia PR #9599)
- artifact: `com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT`
- canonical export manifest Git blob: `8a3e3d6e802e95dbcc7b0bf83a347887b02d6717`
- canonical convergence source Git blob: `b11aff3ace8e77684cfa8cc13fbbb445a05c8a09`
- portable mastery verifier Git blob: `cd54b351f444a497c759a009681cc97d3e79c5ba`

The seven duplicate `com.synexia.rewrite.atom.*` source files formerly retained by this JDK fork are
removed. A3 loads the canonical artifact and verifies its embedded export manifest before applying
any candidate transformation.

The older `com.m3.rewrite.atom.*` package remains temporarily as a compatibility/proof-history
lane for existing M3JDK21 receipts. It is not the A3 recipe authority and must not receive new
reusable transformation logic. New reusable atomizer/patternizer work belongs in Synexia first.

OpenJDK product source remains outside Maven. A3 may write only candidate/evidence output under
`m3/build/**`; product acceptance remains `configure -> make -> jtreg -> runtime`.

## Portable mastery evidence

The slim Synexia A3 artifact also exports
`com.synexia.rewrite.M3RecipeMasteryPortableReceipt`.

This class validates the authority-free `M3_RECIPE_MASTERY_FANIN_V6` transport receipt and
recomputes its content-addressed root. It does not copy or recreate Synexia's mastery laboratory.

M3JDK21 `A3Apply` requires a verified receipt/root pair and records that root in every FILE-local
candidate receipt. OpenJDK product source remains unchanged until the independent JDK
`configure -> make -> jtreg -> runtime` gates and later serial promotion.
