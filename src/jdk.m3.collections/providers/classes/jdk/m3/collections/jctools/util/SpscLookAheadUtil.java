package jdk.m3.collections.jctools.util;

@SuppressWarnings("missing-explicit-ctor")
@InternalAPI
public class SpscLookAheadUtil
{
    public static final int MAX_LOOK_AHEAD_STEP = Integer.getInteger("jctools.spsc.max.lookahead.step", 4096);

    public static int computeLookAheadStep(int actualCapacity)
    {
        return Math.min(actualCapacity / 4, MAX_LOOK_AHEAD_STEP);
    }
}
