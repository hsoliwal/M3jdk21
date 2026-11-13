# Translation receiver-map test syntax repair — 2026-10-11

## Failure evidence

The M3 foundation-contracts job at run 38124027510 fails compiling
`m3/core/test/M3TranslationReceiverMapTest.java`. The final `List.of(` argument
list contains three expected TSV row prefixes, but the third item is followed
immediately by the loop body: the closing `)` is missing. Javac reports
`')' or ',' expected` at the first statement in that loop and cascading
parse errors below it.

## Minimal repair

Close the `List.of(...)` argument list after the third row prefix. No
assertions, field names, mapping semantics, production sources, or interfaces
change.

## Verification

Run the foundation-contracts workflow and require its strict Java 21 compile
and receiver-map test to pass. This repair does not address the independent
Maven dependency-resolution failure in the migration-recipes reactor.
