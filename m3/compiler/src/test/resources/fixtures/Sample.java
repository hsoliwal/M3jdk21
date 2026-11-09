class Sample {
  void sample(Object a, Object b) {
    if (a != null && b == null) { System.out.println(a); }
    if (a instanceof String) { System.out.println(a); }
    if (a != null && sideEffect()) { System.out.println(a); }
    String text = "if (a == null) {}";
    String decoy = """
        if (b != null) { impossible(); }
        """;
    if (decoy.length() < 0) throw new AssertionError();
    if (text.isEmpty()) throw new AssertionError();
  }
  boolean sideEffect() { return true; }
}
class Sibling {
  void sibling(Object x) {
    if (x == null) { System.out.println("null"); }
  }
}
