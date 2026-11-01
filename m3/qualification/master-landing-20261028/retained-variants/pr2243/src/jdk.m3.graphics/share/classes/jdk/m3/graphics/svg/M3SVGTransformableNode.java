package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public abstract class M3SVGTransformableNode extends M3SVGNode {
    static { M3Library.staticLoad(); }

    public M3SVGTransformableNode(long ptr) {
        super(ptr);
    }
}