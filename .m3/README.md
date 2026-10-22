# M3JDK21 public receiver — executed postimages only

Synexia (`hsoliwal/com.synexia`) exclusively owns every Maven/OpenRewrite
recipe, task crate, transformation template, mastery fixture, recipe runner and
recipe execution. M3JDK21 receives only qualified **executed** Java/JNI/native
postimages and data-only receipts binding recipe ID, source revision, target
preimage/postimage hashes, licensing, and completed target-specific gates.

Historical `.m3/openrewrite-recipes/`, `.m3/task-crates/`,
`.m3/atomize-patternize/` and related sidecars are migration residue.
Their presence is **not** permission for new recipe authoring, installation,
copying or execution in this public repository. Do not run the historic
OpenRewrite recipe commands here as a new delivery workflow.

Migration is source-sealed and non-destructive: inventory exact blob hashes and
callers, transfer the canonical recipe sources and their tests to Synexia,
run/qualify them there, then deliver reviewed product postimages and remove
redundant public recipe copies by an independently reviewed cleanup PR.
Do not silently delete provenance or change OpenJDK licensing.

Target-side work remains authoritative for the actual JDK build, jtreg, API/ABI,
JNI/GC/native correctness, installed-image testing and runtime acceptance.
A recipe identifier in a receipt is a provenance pointer, not an executable recipe.
