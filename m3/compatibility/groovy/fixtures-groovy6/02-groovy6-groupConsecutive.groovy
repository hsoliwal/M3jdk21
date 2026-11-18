// Groovy 6-only reference for an operation M3 already represents as chunkBy.
def input = [1, 1, 2, 2, 1, null, null]
def groups = input.groupConsecutive()
assert groups == [[1, 1], [2, 2], [1], [null, null]]
return groups.toString()
