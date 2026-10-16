# Byte-identical canonical-entry verification fixture

These three files are the exact Maven recipe-reuse entry from Synexia commit de17ba9b12fda37f00854e7ab1e59ec526c34191, path synexia-openrewrite-recipes/crates/m3-four-library-bags/receiver. The separate receiver-hosted workflow checks all three Git blob identities before invoking Maven. This fixture tests the same entry when private canonical runners are unavailable; it does not claim that the canonical repository's own workflow passed.

The entry locks the public receiver's complete tested recipe tree and performs fresh acquisition, actual Maven recipe tests and exact fixed-point delivery. It is not another source transformer. The complete runtime, recipe and generated-output proof remains in the main counted collection workflow.
