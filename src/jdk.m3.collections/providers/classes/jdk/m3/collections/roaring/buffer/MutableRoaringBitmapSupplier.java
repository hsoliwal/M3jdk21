/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package jdk.m3.collections.roaring.buffer;

import jdk.m3.collections.roaring.BitmapDataProvider;
import jdk.m3.collections.roaring.BitmapDataProviderSupplier;

/**
 * A {@link BitmapDataProviderSupplier} providing {@link MutableRoaringBitmap} as
 * {@link BitmapDataProvider}
 *
 * @author Benoit Lacelle
 *
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableRoaringBitmapSupplier implements BitmapDataProviderSupplier {

  @Override
  public BitmapDataProvider newEmpty() {
    return new MutableRoaringBitmap();
  }
}
