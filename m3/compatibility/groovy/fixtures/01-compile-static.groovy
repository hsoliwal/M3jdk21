import groovy.transform.CompileStatic
@CompileStatic
class StaticCapitalization {
    static String apply(String input) { input.capitalize() }
}
return StaticCapitalization.apply('hello')
