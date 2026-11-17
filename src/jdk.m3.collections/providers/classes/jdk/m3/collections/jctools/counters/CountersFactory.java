package jdk.m3.collections.jctools.counters;

import jdk.m3.collections.jctools.util.UnsafeAccess;

/**
 * @author Tolstopyatov Vsevolod
 */
public final class CountersFactory
{

    private CountersFactory()
    {
    }

    public static FixedSizeStripedLongCounter createFixedSizeStripedCounter(int stripesCount)
    {
        if (UnsafeAccess.SUPPORTS_GET_AND_ADD_LONG)
        {
            return new FixedSizeStripedLongCounterGetAndAdd(stripesCount);
        }
        else
        {
            return new FixedSizeStripedLongCounterCompareAndSwap(stripesCount);
        }
    }

    public static FixedSizeStripedLongCounter createFixedSizeStripedCounterV6(int stripesCount)
    {
        return new FixedSizeStripedLongCounterCompareAndSwap(stripesCount);
    }

    public static FixedSizeStripedLongCounter createFixedSizeStripedCounterV8(int stripesCount)
    {
        return new FixedSizeStripedLongCounterGetAndAdd(stripesCount);
    }
}
