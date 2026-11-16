package jdk.m3.collections.disruptor;

/**
 * Batch rewind strategy that always rewinds
 */
@SuppressWarnings("missing-explicit-ctor")
public class SimpleBatchRewindStrategy implements BatchRewindStrategy
{
    @Override
    public RewindAction handleRewindException(final RewindableException e, final int retriesAttempted)
    {
        return RewindAction.REWIND;
    }
}
