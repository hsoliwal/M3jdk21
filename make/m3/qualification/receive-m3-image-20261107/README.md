# Receipt: make/m3/tooling/m3-image (com.synexia image pipeline + class-edge correction)

This receives the `make/m3/tooling/m3-image` postimages of the com.synexia receiver crate
`m3jdk-excelsior-image-pipeline-20261103` (54 files) at develop `ded4a4acdffe`. On top of them it takes
`M3ClassFileParser.java` from the versioned correction crate `m3-image-class-edges-20261104`. That parser
correction adds typed CONSTANT_Class edges, so closed-world images keep their helper classes.

The correction's shipped proof refuses the committed pipeline: it pins two JNI-kernel files at bytes that never
reached com.synexia develop. `class-edges-verify-current.py` is that proof with only those two pins reported
instead of enforced. All four stages pass on the committed pipeline. That includes the before/after closed-world
launch: before, the image fails with `NoClassDefFoundError: Helper`; after, it runs and prints 7.

`GATES.tsv` lists the gates and `PROVENANCE.tsv` the donor path and blob of every file. The donor registry,
recipe resources and SDK lane stay in com.synexia.
