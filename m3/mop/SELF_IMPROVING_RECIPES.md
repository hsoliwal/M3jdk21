<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0
-->

# Machine Oriented Programming (MOP): the self-improving recipe invariant

**Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project.**
Original author / project lead: Hitesh Soliwal (Synexia). First-party M3/Synexia
work; see `M3-SYNEXIA-RIGHTS.md` and `M3-SYNEXIA-NOTICE.md` at the repository root.

## The four pillars

Machine Oriented Programming, as defined by Hitesh Soliwal (Synexia), works on code
through four mechanical pillars, each carried out by recipes:

1. **Atomize**: decompose source into typed, addressable atoms (repository, module,
   package, file, type, member, statement) with hashes, spans and contract facts.
2. **Patternize**: recognize the patterns the atoms instantiate (algorithm categories,
   design patterns, structural clusters), so known structure is reused, not regenerated.
3. **Annotate**: record the recognized roles and contracts on the code itself
   (pattern roles, interface contracts, scope), so machines read meaning from structure.
4. **Document**: derive and keep documentation (Javadoc, interface skeletons, design
   records) converged with the code, so documentation is primary semantic memory.

The invariant below makes the recipes that implement these pillars improve themselves.

## The invariant

> Each time an LLM or a coding agent corrects a recipe, the recipe records that
> correction as memory for the next LLM or coding agent. The recipe records the
> improvement.

In Machine Oriented Programming a change to code is the output of a recipe, and a
recipe is improved, never bypassed. Knowledge an agent gains while fixing a recipe
does not stay in a chat transcript or a model's context: it is written into the
recipe's own memory, in the same commit as the fix, where the next agent reads it
before it touches the recipe again. The recipe set therefore improves itself with
every correction, and every improvement is attributable and replayable.

## The record

Every recipe crate carries an append-only `RECIPE_MEMORY.tsv` next to its sources:

| Column | Content |
| --- | --- |
| `utc` | time of the correction |
| `recipe` | fully qualified recipe id |
| `trigger` | what exposed the defect: failing gate, real-world application row, review, user |
| `observed` | the exact gate or test output line that failed (machine text, not a paraphrase) |
| `correction` | commit sha and diff hash of the fix |
| `corrected_by` | agent, model or person who made the fix |
| `guard` | the regression fixture or test added so the defect cannot return |
| `lesson` | one machine-checkable sentence for the next agent |

## The rules

1. **Read before change.** An agent reads `RECIPE_MEMORY.tsv` before it changes or
   applies the recipe.
2. **Same commit.** A correction lands only together with its memory row and its
   guard; a fix without a row is incomplete.
3. **Append only.** Rows are never edited or removed; a later row may supersede an
   earlier lesson by citing it.
4. **Machine text.** `observed` and `lesson` are exact and checkable (a gate line, a
   fixture name, a contract clause), so a recipe or a CPU tool can act on them
   without an LLM.
5. **Automatic where possible.** The recipe compiler and the optional LLM hook append
   the row themselves when they repair a recipe; the TSV schema gate validates the
   file like every other ledger.
6. **Proof decides.** A correction proposed by an LLM is a candidate until the
   recipe's gates pass (diff, compile, tests, fixed point, real-world rows); only a
   proven correction is recorded as an improvement.

## Why

Agents otherwise rediscover the same failure modes in every session. Recording each
correction in the recipe turns repeated neural reasoning into deterministic,
reusable machine memory: the next agent starts from what the last one learned, the
recipe becomes more exact with each repair, and the improvement history is part of
the code it governs.
