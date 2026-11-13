package java.util;
// Audit-only context fixture; product progress/cancellation requires its own gates.
final class M3SortContext {
 final java.util.function.IntBinaryOperator comparator;
 long work;
 final long limit;
 M3SortContext(java.util.function.IntBinaryOperator comparator) { this(comparator,Long.MAX_VALUE); }
 M3SortContext(java.util.function.IntBinaryOperator comparator,long limit) { this.comparator=comparator;this.limit=limit; }
 int compare(int a,int b){step();return comparator.applyAsInt(a,b);}
 void step(){if(++work>limit) throw new Stop();}
 static final class Stop extends RuntimeException {}
}
