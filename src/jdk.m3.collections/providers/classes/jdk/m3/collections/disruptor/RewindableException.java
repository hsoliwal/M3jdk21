package jdk.m3.collections.disruptor;

/**
 * A special exception that can be thrown while using the {@link BatchEventProcessor}.
 * On throwing this exception the {@link BatchEventProcessor} can choose to rewind and replay the batch or throw
 * depending on the {@link BatchRewindStrategy}
 */
@SuppressWarnings("serial")
public class RewindableException extends Throwable
{
    /**
     * @param cause The underlying cause of the exception.
     */
    public RewindableException(final Throwable cause)
    {
        super("REWINDING BATCH", cause);
    }
}
