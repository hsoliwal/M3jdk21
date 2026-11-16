package jdk.m3.collections.roaring;

public interface WordStorage<T> {

  T add(char value);

  boolean isEmpty();

  T runOptimize();
}
