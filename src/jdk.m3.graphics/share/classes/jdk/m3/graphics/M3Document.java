package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;

public class M3Document extends M3RefCnt {
    static { M3Library.staticLoad(); }
    public M3Document(long ptr) {
        super(ptr);
    }

    /**
     * <p>Create a PDF-backed document, writing the results into a WStream.</p>
     *
     * <p>PDF pages are sized when they are created, via beginPage().</p>
     *
     * @param stream  Where the PDF is written
     * @return        PDF Document
     */
    public static M3Document makePDF(M3WStream stream) {
        return makePDF(stream, null, null, null, null, null, null, 0, 0);
    }
    /**
     * <p>Create a PDF-backed document, writing the results into a WStream.</p>
     *
     * <p>PDF pages are sized when they are created, via beginPage().</p>
     *
     * @param stream        Where the PDF is written
     * @param title         Title of the document.
     * @param author        Author of the document.
     * @param subject       Subject of the document.
     * @param keywords      Keywords for the document.
     * @param creator       Creator of the document.
     * @param producer      Producer of the document.
     * @param creationDate  Creation date of the document.
     * @param modifiedDate  Modification date of the document.
     * @return              PDF Document
     */
    public static M3Document makePDF(M3WStream stream, String title, String author, String subject, String keywords, String creator, String producer, long creationDate, long modifiedDate) {
        M3Stats.onNativeCall();
        return new M3Document(_nMakePDF(stream._ptr, title, author, subject, keywords, creator, producer, creationDate, modifiedDate));
    }

    /**
     * <p>Begin a new page for the document, returning the canvas for that page.
     * The canvas is owned by the document, and must not be used after endPage()
     * or close() is called.</p>
     *
     * @param width   width of the page
     * @param height  height of the page
     * @return        Canvas for the page
     */
    public M3Canvas beginPage(float width, float height) {
        M3Stats.onNativeCall();
        return new M3Canvas(_nBeginPage(_ptr, width, height), false, this);
    }

    /**
     * <p>Call endPage() when the content for the current page has been drawn
     * (into the canvas returned by beginPage()).</p>
     */
    public void endPage() {
        M3Stats.onNativeCall();
        _nEndPage(_ptr);
    }

    @Override
    public void close() {
        if (_ptr == 0) return;
        M3Stats.onNativeCall();
        _nClose(_ptr);
        super.close();
    }

    /**
     * <p>Call abort() to stop writing the document and discard the output.</p>
     */
    public void abort() {
        M3Stats.onNativeCall();
        _nAbort(_ptr);
    }

    public static native long _nMakePDF(long streamPtr, String title, String author, String subject, String keywords, String creator, String producer, long creationDate, long modifiedDate);
    public static native long _nBeginPage(long ptr, float width, float height);
    public static native void _nEndPage(long ptr);
    public static native void _nClose(long ptr);
    public static native void _nAbort(long ptr);
}
