# M3 bulk executor port

This receiver ports the Synexia JDK-shaped bulk scheduler into the real M3JDK21 product tree as a
target-owned internal `java.base` contract.

## Canonical authoring source

- Synexia recipe PR: hsoliwal/com.synexia#9892
- sealed recipe revision: `b9c275cb9e0ef0defb665d00e831ce0f9cfef618`
- packet head used by this receiver: `27cb1b974b5e40899a11fd5e10f636274cbbfb11`
- TornadoVM verified RV32IM provider: hsoliwal/synexia-tornadovm#7,
  merge `3bf826714a48d7fdf346ffde8a0fc25c525572c5`

The target source is independently authored under the M3JDK/OpenJDK license boundary. No Synexia
runtime dependency is introduced.

## Contract

```text
BulkTask
  reviewed operation id
  bounded one-dimensional work geometry
  logical input/output names
      |
      v
BulkExecutor
  provider-neutral execution
      |
      v
BulkExecution
  immutable outputs
  provider id
  SHA-256 proof receipt
```

Arbitrary `Runnable`/lambda offload is intentionally outside this contract.

## Proof order

1. migration-recipe receiver JUnit and fixed-point dry run;
2. stock-JDK `--patch-module java.base` compile/runtime smoke;
3. full M3JDK release image build;
4. focused jtreg;
5. provider integration;
6. physical TornadoVM CPU-parity proof;
7. resource/performance evidence before any speed claim.

The first four are wired in `.github/workflows/m3-bulk-executor-port.yml`. Provider/hardware
promotion remains separate.
