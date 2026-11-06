package com.m3.lib.core.util;

import com.m3.lib.core.JsonFactory;
import com.m3.lib.core.JsonGenerator;

/**
 * Simple interface to allow adding decorators around {@link JsonGenerator}s.
 *
 * @since 2.16
 */
public interface JsonGeneratorDecorator
{
    /**
     * Allow to decorate {@link JsonGenerator} instances returned by {@link JsonFactory}.
     * 
     * @since 2.16
     * @param factory The factory which was used to build the original generator
     * @param generator The generator to decorate. This might already be a decorated instance, not the original.
     * @return decorated generator
     */
    JsonGenerator decorate(JsonFactory factory, JsonGenerator generator);
}
