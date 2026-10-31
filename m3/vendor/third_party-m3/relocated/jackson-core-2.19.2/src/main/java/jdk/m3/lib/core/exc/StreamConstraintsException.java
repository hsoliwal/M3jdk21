package jdk.m3.lib.core.exc;

import jdk.m3.lib.core.JsonLocation;
import jdk.m3.lib.core.JsonProcessingException;

/**
 * Exception type used to indicate violations of stream constraints
 * (for example {@link jdk.m3.lib.core.StreamReadConstraints})
 * when reading or writing content.
 *
 * @since 2.15
 */
public class StreamConstraintsException
    extends JsonProcessingException
{
    private final static long serialVersionUID = 2L;

    public StreamConstraintsException(String msg) {
        super(msg);
    }

    public StreamConstraintsException(String msg, JsonLocation loc) {
        super(msg, loc);
    }
}
