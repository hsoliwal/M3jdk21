package jdk.m3.collections.jctools.counters;

import static jdk.m3.collections.jctools.util.UnsafeAccess.UNSAFE;

/**
 * Wait-free implementation of striped counter using
 * Java 8 Unsafe intrinsics (lock addq and lock xchg).
 *
 * @author Tolstopyatov Vsevolod
 */
class FixedSizeStripedLongCounterGetAndAdd extends FixedSizeStripedLongCounter
{

    public FixedSizeStripedLongCounterGetAndAdd(int stripesCount)
    {
        super(stripesCount);
    }

    @Override
    protected void inc(long[] cells, long offset, long delta)
    {
        UNSAFE.getAndAddLong(cells, offset, delta);
    }

    @Override
    protected long getAndReset(long[] cells, long offset)
    {
        return UNSAFE.getAndSetLong(cells, offset, 0L);
    }
}
