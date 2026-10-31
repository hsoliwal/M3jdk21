# Publication and toolchain evidence, 2026-10-06

PR #189 restored 443 exact historical files. An external merge included that historical head at 04:21:13 UTC. The CI repair was subsequently materialized, tested and appended to its branch as commit `293961a4cbf86c1f5afd5466e97ccb15a847186f`; the actual readback correctly records that this later commit is outside the closed PR.

The new immutable master `d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2` retains all 443 restored files. The three CI and six retention target preimages match exactly. The independently reconstructed and GitHub-read-back intermediate tree `cee56051e8a0762dc6ae3c53ddc57a82d4fb4730` combines those nine modifications and 588 additions while preserving every unrelated entry. This intermediate tree is not the final source-binding tree or a published commit. The final follow-up adds its separately executed mapping and publication evidence.

The GCC report preserves the exact CI acquisition failure for `gcc-10` and `g++-10` version `10.4.0-4ubuntu1~22.04`. It does not substitute another compiler version. Four official snapshot metadata attempts timed out before receiving bytes; that is a transport observation, not proof that the packages cannot exist. No snapshot timestamp, package set, native build, or rebuilt JDK is qualified by that report. These six audit documents retain acquisition call metadata, observed hashes and primary source URLs; they do not constitute a complete vendored GCC source or binary distribution.

Historical execution, finite current compiler/JUnit results, synthetic retention fixtures, actual repository tree readbacks and pending native/toolchain admission remain separate evidence scopes.
