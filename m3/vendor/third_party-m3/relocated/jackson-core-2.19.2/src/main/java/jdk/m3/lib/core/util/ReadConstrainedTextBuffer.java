package jdk.m3.lib.core.util;

import jdk.m3.lib.core.StreamReadConstraints;
import jdk.m3.lib.core.exc.StreamConstraintsException;

public final class ReadConstrainedTextBuffer extends TextBuffer {

    private final StreamReadConstraints _streamReadConstraints;

    public ReadConstrainedTextBuffer(StreamReadConstraints streamReadConstraints, BufferRecycler bufferRecycler) {
        super(bufferRecycler);
        _streamReadConstraints = streamReadConstraints;
    }

    /*
    /**********************************************************************
    /* Convenience methods for validation
    /**********************************************************************
     */

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateStringLength(int length) throws StreamConstraintsException
    {
        _streamReadConstraints.validateStringLength(length);
    }
}
