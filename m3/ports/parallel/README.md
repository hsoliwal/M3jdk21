<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/ports/parallel — `m3.parallel`

The M3 data-parallel launch API (leaf P1 of `m3/docs/m3sdk/m3-parallel-vcpu-design.md`): `Backend`, `IntBuffer`,
`KernelId`, `Grid1D`, `Transfer`, `Plan`, `Result`, `Verified`, plus the `spi` package for native providers. The
pure-Java reference backend (`"java"`) is always available and is the parity oracle for every other backend.

```
mvn -B -ntp -f m3/ports/parallel/pom.xml install    # 31 tests
```
