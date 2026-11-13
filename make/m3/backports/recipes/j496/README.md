# JEP 496 — ML-KEM Java-21 candidate packet

Status: **candidate postimages present; reusable recipe seal and executable proof pending**.
Promotion is **not authorized**.

## Exact upstream authority

- named-key prerequisite JDK-8340327:
  `3f53d571343792341481f4d15970cdc0bcd76a5e`
- SHAKE/XOF prerequisite JDK-8338587:
  `c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c`
- JEP 496 / JDK-8298390:
  `13987b4244614d594dc8f94c288eddb6239a066f`
- ACVP test-mechanics reference JDK-8342442:
  `f400896822c2704d8e7c66afc1efa8a4fa91acb6`

The named-key framework is present in the receiving tree. The Java-21 SHAKE/XOF adaptation is
materialized but remains unverified because hosted Actions currently starts no jobs.

## Materialized candidate

The candidate branch contains the pinned JEP 496 implementation leaves:

- `ML_KEM.java`;
- `ML_KEM_Impls.java`;
- `SHA3Parallel.java`;
- exact ML-KEM service registration in `SunJCE`;
- ML-KEM-512/768/1024 constants in `NamedParameterSpec`;
- FIPS 203 ML-KEM OIDs in `KnownOIDs`.

The three constants are an explicit opt-in Java-21 library API extension. They are not presented as
stock Java-21 SE surface.

## Java-21 focused FIPS 203 proof

The later JDK ACVP harness is not copied wholesale. Java 21 already has
`jdk.test.lib.json.JSONValue`, but lacks the later generic `FixedSecureRandom`,
`SeededSecureRandom`, and byte-array assertion helpers.

This packet therefore retains the exact upstream FIPS 203 JSON vectors and adds one focused
`MLKEMKnownAnswer.java` test with a tiny local deterministic `SecureRandom`. It verifies exact
key-generation bytes, encapsulation bytes, shared secrets, decapsulation secrets and complete
deterministic-random consumption for ML-KEM-512/768/1024.

The broad post-21 `provider/all/Deterministic.java` harness and generic ACVP launcher are not
dependencies of this Java-21 candidate.

## Recipe-first boundary

Current source changes are candidate postimage evidence until the reusable hash-pinned
`M3Jep496MlKemBackportRecipe` reproduces them from the sealed SHAKE-branch preimages, rejects drift,
and reaches a no-op second pass. After-the-fact recipe existence alone does not authorize
promotion.

## Required proof

1. prerequisite named-key proof;
2. SHAKE/XOF recipe + JDK build/jtreg proof;
3. JEP 496 recipe JUnit and fixed point;
4. Java-21 JDK image build;
5. focused FIPS 203 known-answer jtreg;
6. KEM/key-factory/provider/OID/public-API checks;
7. wider relevant security jtreg;
8. exact canonical-tree readback.

Native/SIMD/JIT optimization is deferred until the Java implementation is a proven semantic oracle.
