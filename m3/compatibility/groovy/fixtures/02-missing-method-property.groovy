class MissingObject {
    def methodMissing(String name, args) { "m:${name}:${args.size()}" }
    def propertyMissing(String name) { "p:${name}" }
}
def x = new MissingObject()
return x.absentMethod(1, 2) + '|' + x.absentProperty
