package jdk.m3.graphics.svg;

import jdk.m3.graphics.*;
import jdk.m3.graphics.impl.*;

public abstract class M3SVGContainer extends M3SVGTransformableNode {
    static { M3Library.staticLoad(); }

    public M3SVGContainer(long ptr) {
        super(ptr);
    }
}