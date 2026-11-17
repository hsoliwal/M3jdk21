package jdk.m3.collections.hppc;

/**
 * Marker interface for containers that can check if they contain a given object in at least time
 * <code>O(log n)</code> and ideally in amortized constant time <code>O(1)</code>.
 */
@jdk.m3.collections.hppc.Generated(
    date = "2026-11-04T09:22:42+0100",
    value = "KTypeLookupContainer.java")
public interface IntLookupContainer extends IntContainer {
  public boolean contains(int e);
}
