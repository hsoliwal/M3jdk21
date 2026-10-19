# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Classifies the method rows of methods.awk into collection-shaped responsibilities (first matching rule wins; the
# other matching rules are listed as "also"). Rows without a collection-shaped token are counted, not listed.
# Output columns: source, responsibility, leetcode_technique, target_collection, status, owner, adoption, also, lines.
function owner(c) {
	if (c ~ /^nebula\./) return "nebula-grid"
	if (c ~ /^swing\./) return "m3jdk-swing"
	if (c ~ /^m3\.(M3Column|M3Sort|M3Filter|M3RowMask|M3RowView|M3RowWindow|M3ProxyModel|M3RowBinding|M3RowChange|M3Text|M3Token|M3Collation|M3Checks|M3CellText|M3Columns|M3HeaderDecoration|M3Messages)/) return "sort-filter"
	if (c ~ /^widgets\.(TreeViewport|VirtualTree)/) return "claude-tree"
	if (c ~ /^widgets\.(TableViewport|VirtualCellLanes|VirtualPageSource|VirtualPageHost|VirtualBlock)/) return "swt-table"
	if (c ~ /^widgets\.(ListViewport|ComboViewport|VirtualListWidth|VirtualListModel)/) return "swt-list"
	if (c ~ /^widgets\.M3/) return "api-enrichment"
	if (c ~ /^internal\.(render|gpu)/) return "render-modes"
	if (c ~ /^internal\.laf/) return "swt-allwidgets"
	return "swt-allwidgets"
}
function extracted(c) {
	return c ~ /^widgets\.(VirtualTreeSiblingIndex|VirtualRowExtents|VirtualCellLanes|VirtualBitLanes|VirtualBlockStore|VirtualBlockCache|VirtualGrowth|VirtualItemStorage|VirtualScrollMetrics|VirtualRowWindowPlan|VirtualItemState|VirtualTreeTopology|VirtualTreeVisibleProjection|VirtualTreeResidentPlan|VirtualTreeViewport|VirtualSelectionModel|VirtualViewportPlanner|VirtualListModel|VirtualListWidth|ViewportScrollLayout|ViewportScrollBarGeometry|ViewportPlaneGeometry)$|^internal\.ViewportRuntime$|^nebula\.(GridRowTree|GridRowIndex|GridSelectionList|GridCellSelectionList)$/
}
function add(r, t, k) { R[++nr] = r; T[nr] = t; K[nr] = k }
BEGIN {
	FS = OFS = "\t"
	add("prefix sums (Fenwick tree)", "Fenwick/BIT point update + prefix query (LC 307, 315)", "M3SparseExtents")
	add("order statistics (AVL with subtree counts)", "balanced BST with order statistics (LC 315, 1649; GfG order-statistic tree)", "M3OrderStatisticForest")
	add("bitmask lanes (word at a time)", "bit manipulation: popcount, lowest set bit, word scans (LC 191, 136, 338)", "M3BitMask")
	add("range set (sorted int pairs)", "interval merging + binary search (LC 56, 57, 352)", "M3RangeSet")
	add("diff / patch (window delta or snapshot diff)", "LIS/LCS runs (LC 300, 1143); two pointers over windows", "M3ListDiff")
	add("bounded cache (LRU / CLOCK)", "LRU cache (LC 146); CLOCK second chance", "M3BoundedCache")
	add("permutation sort (index lanes)", "sort a permutation of row indices (LC 912, 75; counting / cyclic sort)", "M3PermutationIndex")
	add("bitmask filter (rank/select over masks)", "bit manipulation filter + popcount rank (LC 338, 1356)", "M3BitMask")
	add("int-pair keyed map", "hashing packed int pairs (LC 1, 149)", "M3LongLongHashMap")
	add("binary search on sorted lanes / offsets", "binary search on a monotone array (LC 35, 704, 2187)", "M3SparseExtents")
	add("sliding window over rows", "sliding window (LC 239, 643)", "M3WindowView")
	add("tree flattening (expand masks)", "DFS preorder flattening (LC 114, 430)", "M3FlatteningView")
	add("prefix sums", "prefix sums (LC 303, 560)", "M3SparseExtents")
	add("ring buffer", "circular queue (LC 622, 641)", "M3LongArrayDeque")
	add("growable lanes", "amortized dynamic array (LC 705-707 design)", "M3LaneGrowth")
	P[1] = "& -[a-z]|& \\(-|[Ff]enwick"
	P[2] = "rotateLeft|rotateRight|rebalance"
	P[3] = "bitCount|numberOfTrailingZeros|numberOfLeadingZeros|lowestOneBit|>>> 5|>>> 6|& 31\\)|& 63\\)|1L << |-1 << |-1L << "
	P[4] = "addRange|removeRange|rangeStart|rangeEndExclusive|complement"
	P[5] = "[Dd]iff|[Pp]atch|retiredHead|retiredTail|insertedHead|insertedTail|longestIncreasing"
	P[6] = "evict|[Ss]tamp|clock|[Ll]ru|LRU|victim"
	P[7] = "[Pp]ermutation|Comparator|Arrays\\.sort|\\.sort ?\\(|[Ss]ortLane"
	P[8] = "[Ff]ilter.*[Mm]ask|[Mm]ask.*[Ff]ilter|visibleMask|rowMask"
	P[9] = "<< 32|HashMap<Long|Long\\.valueOf\\(\\(long\\)"
	P[10] = "binarySearch|>>> 1\\)|low \\+ high|lo \\+ hi|middle"
	P[11] = "overscan|firstVisible|visibleCount|paintStart|[Ww]indow|[Tt]opIndex"
	P[12] = "preorder|EXPANDED|isExpanded|visibleRow|[Ff]latten|[Pp]rojection"
	P[13] = "prefix|offsetOf|extentBefore|cumulative"
	P[14] = "% capacity|& mask\\)|head = \\(|tail = \\("
	P[15] = "copyOf|arraycopy"
}
{
	body = $2 " " $5
	first = 0; also = ""
	for (i = 1; i <= nr; i++) {
		if (body ~ P[i]) { if (!first) first = i; else also = also (also == "" ? "" : "; ") R[i] }
	}
	if (!first) { skipped++; next }
	if (first == 15 && $2 !~ /grow|ensure|[Cc]apacity|resize|reserve/) { skipped++; next }
	target = K[first]
	if ($1 ~ /^nebula\.GridRowTree/ && first <= 2) target = "M3RowTree"
	if ($1 ~ /^swing\./ && first == 1) target = "M3FenwickForest (planned; wave 4)"
	if ($1 ~ /^widgets\.VirtualBitLanes/ && first == 3) target = "M3BitLanes"
	if ($1 ~ /^widgets\.VirtualBlockCache/ && first == 6) target = "M3BlockCache"
	loops = body ~ /(for|while) \(/
	arrays = body ~ /[a-zA-Z_]+ ?\[[^\]]+\]/
	kind = (loops && arrays) || body ~ P[3] || body ~ P[1] ? "OWNS_ALGORITHM" : "CONSUMER"
	status = extracted($1) ? "EXTRACTED_BY_RECIPE" : kind == "CONSUMER" ? "CONSUMER_ADOPT" : (target ~ /M3BitMask|M3PermutationIndex|planned/ ? "PLANNED" : "TARGET_EXISTS_ADOPT")
	own = owner($1)
	adopt = status == "EXTRACTED_BY_RECIPE" ? "none (same code, folded copy)" : own ": switch " $1 "#" $2 " onto " target " (BCP)"
	print $1 "#" $2, R[first], T[first], target, status, own, adopt, kind, also, $4
}
END { printf "%d methods without a collection-shaped token\n", skipped > "/dev/stderr" }
