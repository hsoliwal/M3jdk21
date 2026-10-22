# Qualified parser overlay

Build from the owning checkout: `mvn -f m3/reactor.xml verify`. The reactor compiles this qualified module before the recipe consumer, so a clean checkout needs no preinstalled private artifact. `mvn -f m3/reactor.xml -Pinventory verify` runs the actual owning inventory CLI. The original SnippetTooltip and RoundScale parser tests default to the owning repository root.

Only three pinned OpenRewrite 8.90.4 classes are adapted. Source/dependency checksum preflight precedes strict `-Xlint:all -Werror` compilation. Fifteen tests retain donor helper behavior, actual isolated parser loading, exact LF/CRLF/CR/EOF print preservation, typed Javadoc and original source bytes. Apache-2.0 and MIT attribution are retained; see PROVENANCE.md and third_party licenses. Original Nebula inputs are never normalized or written.
