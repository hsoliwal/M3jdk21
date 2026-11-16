package jdk.m3.collections.disruptor.dsl;

import jdk.m3.collections.disruptor.Sequence;
import jdk.m3.collections.disruptor.SequenceBarrier;

import java.util.concurrent.ThreadFactory;

interface ConsumerInfo
{
    Sequence[] getSequences();

    SequenceBarrier getBarrier();

    boolean isEndOfChain();

    void start(ThreadFactory threadFactory);

    void halt();

    void markAsUsedInBarrier();

    boolean isRunning();
}
