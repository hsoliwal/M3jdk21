# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Emits one row per method or constructor of a Java source: class, method(params), first line, line count, body.
# The body is flattened to one line (tabs and newlines become spaces) so a second pass can classify it by tokens.
# Comments, string and char literals are blanked first, so braces inside them do not disturb the depth count.
# Usage: awk -v cls=<ClassLabel> -f methods.awk File.java
function strip(s) {
	gsub(/\\\\/, "", s); gsub(/\\"/, "", s); gsub(/\\'/, "", s)
	gsub(/"[^"]*"/, "\"\"", s); gsub(/'[^']*'/, "' '", s)
	return s
}
BEGIN { depth = 0; inMethod = 0; inComment = 0 }
{
	line = $0
	if (inComment) { if (line ~ /\*\//) { sub(/^.*\*\//, "", line); inComment = 0 } else next }
	gsub(/\/\*.*\*\//, "", line)
	if (line ~ /\/\*/) { sub(/\/\*.*$/, "", line); inComment = 1 }
	sub(/\/\/.*$/, "", line)
	line = strip(line)
	if (!inMethod && line ~ /^[ \t]*(public |protected |private |static |final |synchronized |abstract |native |default )*[A-Za-z_][A-Za-z0-9_<>,.? \[\]]* [A-Za-z_][A-Za-z0-9_]* ?\([^;]*\)[^;]*\{[ \t]*$/ \
	    && line !~ /^[ \t]*(if|for|while|switch|catch|else|return|new|try|do|synchronized) / && line !~ / class | interface | record | enum /) {
		name = line; sub(/ ?\(.*$/, "", name); sub(/^.* /, "", name)
		params = line; sub(/^[^(]*\(/, "", params); sub(/\).*$/, "", params)
		n = (params ~ /^[ \t]*$/) ? 0 : gsub(/,/, ",", params) + 1
		inMethod = 1; start = NR; body = ""; mdepth = depth
	} else if (!inMethod && line ~ /^[ \t]*(public |protected |private )?[A-Z][A-Za-z0-9_]* ?\([^;]*\)[^;]*\{[ \t]*$/ && line !~ /^[ \t]*(if|for|while|switch|catch|new) /) {
		name = line; sub(/ ?\(.*$/, "", name); sub(/^.* /, "", name); gsub(/[ \t]/, "", name)
		params = line; sub(/^[^(]*\(/, "", params); sub(/\).*$/, "", params)
		n = (params ~ /^[ \t]*$/) ? 0 : gsub(/,/, ",", params) + 1
		inMethod = 1; start = NR; body = ""; mdepth = depth
	}
	opens = gsub(/\{/, "{", line); closes = gsub(/\}/, "}", line)
	depth += opens - closes
	if (inMethod) {
		t = line; gsub(/[\t]+/, " ", t); body = body " " t
		if (depth <= mdepth) {
			printf "%s\t%s(%d)\t%d\t%d\t%s\n", cls, name, n, start, NR - start + 1, body
			inMethod = 0
		}
	}
}
