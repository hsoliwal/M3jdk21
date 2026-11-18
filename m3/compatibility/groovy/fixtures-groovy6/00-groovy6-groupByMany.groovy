// Apache Groovy 6.0.x oracle only; not a Groovy 4/5 fixture.
def words = ['ant', 'bee', 'ape', 'cow']
def keys = { String value -> value.toList().findAll { it in ['a','e','i','o','u'] }.unique() }
def groups = words.groupByMany(keys)
assert groups['a'] == ['ant', 'ape']
assert groups['e'] == ['bee', 'ape']
assert groups['o'] == ['cow']
return groups.entrySet().toList().sort { it.key }.collect { it.key + ':' + it.value.join(',') }.join('|')
