// Two-Function form; value conversion runs for each admitted membership.
def words = ['ab','bc','ca']
def groups = words.groupByMany({ it.toUpperCase() }, { it.toList().unique() })
assert groups['a'] == ['AB','CA']
assert groups['b'] == ['AB','BC']
assert groups['c'] == ['BC','CA']
return groups.entrySet().toList().sort { it.key }.collect { it.key + ':' + it.value.join(',') }.join('|')
