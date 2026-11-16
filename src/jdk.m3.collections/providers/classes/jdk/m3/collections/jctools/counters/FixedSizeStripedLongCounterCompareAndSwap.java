package jdk.m3.collections.jctools.counters;

import static jdk.m3.collections.jctools.util.UnsafeAccess.UNSAFE;

/**
 * Lock-free implementation of striped counter using
 * CAS primitives.
 *
 * @author Tolstopyatov Vsevolod
 */
class FixedSizeStripedLongCounterCompareAndSwap extends FixedSizeStripedLongCounter
{

    public FixedSizeStripedLongCounterCompareAndSwap(int stripesCount)
    {
        super(stripesCount);
    }

    @Override
    protected void inc(long[] cells, long offset, long delta)
    {
        long v;
        do
        {
            v = UNSAFE.getLongVolatile(cells, offset);
        }
        while (!UNSAFE.compareAndSwapLong(cells, offset, v, v + delta));
    }

    @Override
    protected long getAndReset(long[] cells, long offset)
    {
        long v;
        do
        {
            v = UNSAFE.getLongVolatile(cells, offset);
        }
        while (!UNSAFE.compareAndSwapLong(cells, offset, v, 0L));

        return v;
    }
}
