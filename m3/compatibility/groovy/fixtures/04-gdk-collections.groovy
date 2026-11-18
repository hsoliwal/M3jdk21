def values = [1, 2, 3, 4]
def filtered = values.findAll { it % 2 == 0 }
def mapped = filtered.collect { it * it }
def grouped = values.groupBy { it % 2 }
return [filtered, mapped, grouped[0], grouped[1]].toString()
