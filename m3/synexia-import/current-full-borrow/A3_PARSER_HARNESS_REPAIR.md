# A3 in-memory parser harness correction

The A3 test composes one Java snapshot recipe and one Markdown/text snapshot recipe. Its shared helper parsed every input with JavaParser, so the text recipe rejected the Markdown target before its assertions could run.

The hash-pinned candidate changes only test harness parser dispatch: `.java` inputs use JavaParser; `.md` inputs use PlainTextParser; other extensions fail closed. Product sources and recipe output contracts are unchanged.

Preimage SHA-256: `0d219e6c081f5f41d6825ba38ef0b0548a4874401a1401d03665692168e5150b`
Postimage SHA-256: `ff2290abe9ca91af361e2322848edb1ed3ae19efec4b0b7d1322be8411613185`

The dedicated JUnit test applies the existing hash-pinned Java recipe, compares the generated postimage, requires fixed point, and refuses source drift.
