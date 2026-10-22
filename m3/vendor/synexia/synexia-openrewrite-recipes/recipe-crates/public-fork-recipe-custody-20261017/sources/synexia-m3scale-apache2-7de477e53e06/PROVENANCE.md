# Provenance

Bundle: synexia-m3scale
Bundle directory: D:\Code\com.msq\dist\synexia-m3scale-apache2\20260310_111830_remote_apache2_module_synexia-m3scale
Stamped by: M3 Apache-2.0 release hardening lane
Stamped on: 2026-03-10

## Classification
- status: bounded-remote-surface
- summary: Synexia M3Scale bundle assembled from one remote module root and prepared for Apache-2.0 publication.
- release_scope: Bounded Apache-2.0 release candidate built from the filtered synexia-m3scale remote project surface.

## Boundary
- This bundle is a bounded public release candidate.
- It does not blanket-relicense the full workspace.
- It excludes mixed-provenance commercial clone surfaces and donor reference corpora.

## 2026-09-20 Java accelerator lane

The M3 Java accelerator implementation was added as independently written Java 21 source.

- CPU hashing and verification use JDK APIs and remain semantic authority.
- TornadoVM 6.1.0 is referenced only through its public Java API in optional Maven profiles.
- JCuda 12.6.0 is referenced only through its public Java CUDA/NVRTC bindings in optional Maven profiles.
- The embedded NVRTC SHA-256, FNV-1a64, and Hamming64 kernels are newly written for this repository.
- Aparapi 3.0.0 was evaluated as an OpenCL donor but is not linked or packaged.
- No GPU execution result is treated as M3 proof without the configured CPU verification policy.
