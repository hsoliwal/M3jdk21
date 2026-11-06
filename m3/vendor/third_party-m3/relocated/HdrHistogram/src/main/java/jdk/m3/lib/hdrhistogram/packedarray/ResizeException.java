package jdk.m3.lib.hdrhistogram.packedarray;

class ResizeException extends Exception {
    private int newSize;

    ResizeException(final int newSize) {
        this.newSize = newSize;
    }

    int getNewSize() {
        return newSize;
    }
}
