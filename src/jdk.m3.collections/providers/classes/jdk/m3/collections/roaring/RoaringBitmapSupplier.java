/*
 * (c) the authors Licensed under the Apache License, Version 2.0.
 */
package jdk.m3.collections.roaring;

/**
 * A {@link BitmapDataProviderSupplier} providing {@link RoaringBitmap} as
 * {@link BitmapDataProvider}
 *
 * @author Benoit Lacelle
 *
 */
@SuppressWarnings("missing-explicit-ctor")
public class RoaringBitmapSupplier implements BitmapDataProviderSupplier {

  @Override
  public BitmapDataProvider newEmpty() {
    return new RoaringBitmap();
  }
}
