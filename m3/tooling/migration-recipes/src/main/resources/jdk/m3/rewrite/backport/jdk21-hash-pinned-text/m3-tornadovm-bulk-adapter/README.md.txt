# M3 TornadoVM bulk adapter

Optional M3JDK adapter from the internal `jdk.internal.vm.parallel` contract to the verified
`hsoliwal/synexia-tornadovm` RV32IM bulk provider.

The module is **not part of java.base** and is not active in the default M3 Maven reactor. Enable it
with the `m3-tornado` profile after installing the exact Tornado provider dependency.

The adapter accepts one operation identity:

`m3.rv32im.slice`

Inputs:
- `batch`: `Rv32iBatch`;
- `instructionBudget`: positive `Integer`.

Output:
- `batch`: a new executed copy; the caller's input batch remains unchanged.

CPU mode requires `localWork == 0`. Tornado mode requires a positive local work size and returns
only after the provider's existing full CPU/Tornado registers/state/RAM parity gate has issued a
`TORNADO_VERIFIED` receipt.

No arbitrary Runnable/lambda offload, no java.base Tornado dependency, and no speedup claim.
