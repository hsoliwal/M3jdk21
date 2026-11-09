<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/ports/vcpu — `m3.vcpu` and the native vCPU HAL

- Java (leaf P1): the RV32IM + XM3_HALT virtual CPU ported from cpu-gpu onto `m3.parallel`; CPU receipts are
  byte-identical to the donor oracle (26 golden roots). `mvn -B -ntp -f m3/ports/vcpu/pom.xml verify` (34 tests).
- Native (leaves P2 + P3, `native/`): the C99 HAL `m3vcpu_backend_v1`, the CPU backend (scalar / AVX2 / AVX-512,
  runtime ISA dispatch), the OpenCL backend (OpenCL loaded at run time), JNI glue (`com.m3.vcpu.internal.NativeVcpu`)
  and ctests, including bit-exact parity with the donor Java oracle on CPU and OpenCL devices and an import-table
  gate (no vendor runtime linked).

```
cmake -S m3/ports/vcpu/native -B build/m3vcpu -G Ninja -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=ON -DM3VCPU_BUILD_JNI=ON
cmake --build build/m3vcpu
ctest --test-dir build/m3vcpu --output-on-failure
```

The Maven `-Pnative` profile that drives these steps is not wired yet.
