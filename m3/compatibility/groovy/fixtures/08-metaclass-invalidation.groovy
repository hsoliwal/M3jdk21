class Evolving { String identity() { 'before' } }
def before = new Evolving().identity()
Evolving.metaClass.identity = { -> 'after' }
return before + '|' + new Evolving().identity()
