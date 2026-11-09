# Consolidation: m3/tooling/companion folded into the census family (jdk.m3.tooling.atoms)

#2800 had received the com.synexia companion admission planner as its own module and package
(`m3/tooling/companion`, `jdk.m3.tooling.companion`): a one-concept package next to the census family that already
inventories Java API surfaces. The planner is an API census and admission tool (system-module class census, classfile
seal, admission plan), so it joins that family's owner, `m3/tooling/atom-census`, in package `jdk.m3.tooling.atoms`.
The package line is the only source change; the README becomes `COMPANION.md`; `verify.sh` becomes
`verify-companion.sh` beside the module's other `verify-*.sh` gates. The old module root and package are removed.
No class name collides.

| gate (WSL, OpenJDK 21) | result |
|---|---|
| verify-companion.sh | rc=0: plan 26, classfile seal 19, system API census 15, review 26 assertions; output identical to before the move |
| verify-contract.sh (owner, unchanged) | rc=0, checks=31 |
| verify-jni-projection.sh (owner, unchanged) | rc=0, checks=53 |
| every atom-census main source together, javac --release 21 -Xlint:all -Werror | rc=0 |
