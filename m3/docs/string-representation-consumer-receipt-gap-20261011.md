# String representation-consumer receipt gap — 2026-10-11

## Failure observed

The fail-closed STRING closure verifier requires
`m3/compatibility/synexia-string-representation-consumers-receipt.json`,
but that path is absent from the current M3JDK21 tree and no canonical receipt
was found in the current Synexia recipe repository search.

The missing artifact cannot honestly be represented as completed proof.
Existing current-master custody identifies the public String bridge,
AbstractStringBuilder interop, and JNI String shadow boundary as relevant
owners, but does not by itself prove all representation consumers, their
runtime behavior, or promotion readiness.

## Safe recovery

Add an explicit intake receipt that:
- records the exact source revision and Git blob/SHA-256 identities already
  present in the current-master owner receipt;
- names the three initial consumer surfaces for review;
- sets the state to `PENDING_CONSUMER_REVIEW`;
- sets every behavioral and promotion claim to false or pending;
- requires a later recipe-backed consumer inventory and matched-runtime tests
  before any proof or promotion state can advance.

This repairs the missing-artifact inventory without weakening the closure
verifier or pretending that the pending receipt is runtime evidence.

## Verification boundary

The ordinary closure verifier may confirm the required artifact exists, but
the STRING executable gates and `PROMOTION=BLOCKED` remain unchanged. The
receipt is not authorization to promote M3 String.
