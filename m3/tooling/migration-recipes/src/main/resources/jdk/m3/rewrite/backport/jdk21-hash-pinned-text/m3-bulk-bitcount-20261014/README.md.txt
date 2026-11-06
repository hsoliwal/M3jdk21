# M3 bulk bitcount proof

Source-only deterministic Java 21 compiler/differential proof for
`m3.parallel.int-bitcount.v1`. Execute with:

```sh
bash m3/ports/parallel-bulk/verification/verify-bitcount.sh
```

The script compiles real M3JDK `jdk.internal.vm.parallel` classes as an isolated
`java.base` patch, then compiles the real `m3.parallel` sources and both the
provider and independent proof. It executes in normal and `-Xint` modes.

It is not full JDK `make images`, jtreg, Maven JUnit, or physical accelerator evidence.
