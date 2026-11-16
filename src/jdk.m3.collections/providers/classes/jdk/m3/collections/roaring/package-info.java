/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */

/**
 * The jdk.m3.collections.roaring package  provides
 * one class ({@link jdk.m3.collections.roaring.RoaringBitmap}) that   users
 * can rely upon for fast set of integers.
 *
 *
 * <pre>
 * {@code
 *      import jdk.m3.collections.roaring.*;
 *
 *      //...
 *
 *      RoaringBitmap r1 = new RoaringBitmap();
 *      for(int k = 4000; k<4255;++k) r1.add(k);
 *
 *      RoaringBitmap r2 = new RoaringBitmap();
 *      for(int k = 1000; k<4255; k+=2) r2.add(k);
 *
 *      RoaringBitmap union = RoaringBitmap.or(r1, r2);
 *      RoaringBitmap intersection = RoaringBitmap.and(r1, r2);
 *
 *      //...
 *      DataOutputStream wheretoserialize = ...
 *      r1.runOptimize(); // can help compression
 *      r1.serialize(wheretoserialize);
 * }
 * </pre>
 *
 */
package jdk.m3.collections.roaring;
