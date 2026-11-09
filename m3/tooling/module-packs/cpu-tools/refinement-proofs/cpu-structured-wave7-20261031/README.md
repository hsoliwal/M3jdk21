# CPU structured wave7: qualification handoff

Product candidates are exact byte-for-byte generated Java 21 postimages.
No recipe implementation or authoring fixtures belong in receiving products;
only the independent product tests, their source-scoped verification driver,
coverage supplement and this provenance note may accompany the three new
operators. `Synexia` remains the accepted Maven/OpenRewrite recipe owner.

The local source-verification record does not establish full JMOD or jtreg
qualification. Existing target owners, exports and public interfaces remain
unchanged; target integration must preserve develop ancestry and independently
prove applicable runtime and native gates before admission.
