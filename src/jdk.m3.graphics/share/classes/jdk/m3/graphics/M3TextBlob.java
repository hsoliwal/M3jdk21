package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/* M3-ATOM kind=TYPE id=9d3b7fdf469ff05b0f4898f2b02c87485bc73d1abcb7f4a3f81561f85b120cb2 contract=PUBLIC_API patterns=INHERITANCE */
public class M3TextBlob extends M3Managed {
    static { M3Library.staticLoad(); }
    /* M3-ATOM kind=METHOD id=c64a6f9b4025cda1f414e78d64f2039683145b1383ddadb0ba591bf20cb81417 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3TextBlob(long ptr) {
        /* M3-LEAF kind=INVOCATION id=5ff9b40e8112b2afa90f759efacdf21fc13c9194a26400c0d930bb8412e91728 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        super(ptr, /* M3-LEAF kind=FIELDACCESS id=c7aefd0788584bfe4afcf787bd51d33bc68ac0ce8d023821431feba8109e4fc4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _FinalizerHolder.PTR);
    }

    /**
     * Returns conservative bounding box. Uses Paint associated with each glyph to
     * determine glyph bounds, and unions all bounds. Returned bounds may be
     * larger than the bounds of all glyphs in runs.
     *
     * @return  conservative bounding box
     */
    /* M3-ATOM kind=METHOD id=2a543de7aed5a2f9ab138dfbcca83e6c0138ea866f34ad261cf0028670f8fb16 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Rect getBounds() {
        /* M3-LEAF kind=TRY id=d2d282d5173e5aa2533a7f456be687b60acfec869fa95ed41e73bd9c2337fca3 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=deb801003f15b5bd7becb5807a16b9b4c59b02cbbc43595d2c712c585f10a0f2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=472ce9ad87039a3b12bb99fd20fb488b5c29f56445340838c08376ec39579180 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=2d5f73b17798a6087afeb46c90ed01b649a00c6c582630e6af27a41ae3a3c38b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=53ce66437fbf7bd6683a939d2699bbddf2e2adbe5af6cb0774b4a73d0a3453b0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nBounds(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=45ffdcf5a7de704a3670cbccbc9e201ef8928f4c81179f023978ad462ec99f02 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Returns a non-zero value unique among all text blobs.
     *
     * @return  identifier for TextBlob
     */
    /* M3-ATOM kind=METHOD id=f37e34aaeda0f7f659c084d56f06f0c7e687d3d5d62154a891d03f26ea5a0197 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public int getUniqueId() {
        /* M3-LEAF kind=TRY id=fd11de0f2638ed533c1ad194e901bd47db7eaff0aa206402cf118087dfe68003 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=e5ce98e95d3345984473aff05cf4f07ec0a9edbe81ec3e8cc73a4f0479f7edf7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f0557dddf5a8202b96d524f45352be6c46a74eeb8fc809153a770ed686420678 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=11571c221b65c9f0a8861c4d3672dfcb7e9326ffbbd1f403715d0604de06c3b4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=d7ceb14584e9458f696c031f836e19bf2bb6aca76a4f57cef74842fdb32893e0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetUniqueId(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=65203cbc65fa7b6effa6322a20f44f25400787f7d357d519b9d0bc7985587d5b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Returns the number of intervals that intersect bounds.
     * bounds describes a pair of lines parallel to the text advance.
     * The return array size is a multiple of two, and is at most twice the number of glyphs in
     * the the blob.</p>
     *
     * <p>Runs within the blob that contain SkRSXform are ignored when computing intercepts.</p>
     *
     * @param lowerBound lower line parallel to the advance
     * @param upperBound upper line parallel to the advance
     * @return           intersections; may be null
     */
    /* M3-ATOM kind=METHOD id=8041455f1f645d0dad2436fb7468a679bac25677ad6d13b8edcb9e24607757f6 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public float[] getIntercepts(float lowerBound, float upperBound) {
        /* M3-LEAF kind=RETURN id=8c1216d8ba04a645220d9ff032471a274c060e640a36f6f33abb174b5439451e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=66eb315141e75bacb45a661f01139404d24808d81cc0bbfbc30619a4f732224e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 getIntercepts(lowerBound, upperBound);
    }

    /**
     * <p>Returns the number of intervals that intersect bounds.
     * bounds describes a pair of lines parallel to the text advance.
     * The return array size is a multiple of two, and is at most twice the number of glyphs in
     * the the blob.</p>
     *
     * <p>Runs within the blob that contain SkRSXform are ignored when computing intercepts.</p>
     *
     * @param lowerBound lower line parallel to the advance
     * @param upperBound upper line parallel to the advance
     * @param paint      specifies stroking, PathEffect that affects the result; may be null
     * @return           intersections; may be null
     */
    /* M3-ATOM kind=METHOD id=c59c98c67816e074d82f7f442d7e99841f4acdbd0daa66e89e79e867ed1d60f6 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public float[] getIntercepts(float lowerBound, float upperBound, M3Paint paint) {
        /* M3-LEAF kind=TRY id=376c5a638963e85136d897b00dd15a56fb0930537b53ffdafe485108791ac591 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=99de61d23774b7804d3438929d6a2ab064925ac873219b0cbfa4c3d08c05812f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=d09e76a86946f0f055181123e448e96511e7ab0f4fcc73f27635b3e843746cca contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=44f5f789cdb2fd916ae92e9a6355dd5b6df1fde09c1619572f935052253a8e89 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=b5d04867d0340fe4a188ace4bdabd0249ea5d1d3a67a00f81c144ff14a97c665 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetIntercepts(_ptr, lowerBound, upperBound, /* M3-LEAF kind=INVOCATION id=272416ddcb3b1bc1afdcbed99f44af9145f84932f7918f713c93df8c6e21a0d1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(paint));
        } finally {
            /* M3-LEAF kind=INVOCATION id=1c2afde02ac4efb6cf6d73a3073e12c7e7d5654f2de68d91317eeca37898703c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=01424843feb53b1162bae180ceb6a4d803b6938020a9986c2d05c7fab1e1d2a3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(paint);
        }
    }

    /**
     * Returns a TextBlob built from a single run of text with x-positions and a single y value.
     * Returns null if glyphs is empty.
     *
     * @param glyphs  glyphs drawn
     * @param xpos    array of x-positions, must contain values for all of the glyphs.
     * @param ypos    shared y-position for each glyph, to be paired with each xpos.
     * @param font    Font used for this run
     * @return        new TextBlob or null
     */
    /* M3-ATOM kind=METHOD id=0d52d43171c7cc0b19e49176a75bf02624a0050c99180b4d77de2ee59d464ba2 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public static M3TextBlob makeFromPosH(short[] glyphs, float[] xpos, float ypos, M3Font font) {
        /* M3-LEAF kind=TRY id=6e35b4d1607cc438559b984b93b97b3c9cc1b6825f44e547454c75c254f1091b contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=ASSERTION id=fbb040db5fb43b1d40672222ccc2762fc40e19c32a76dae76ca9736d786d1aa6 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert /* M3-LEAF kind=FIELDACCESS id=fd1f7613f21a77b7100aa0693c7bf2f4b7380f8f2c95c95c53000b50c33126cb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 glyphs.length == /* M3-LEAF kind=FIELDACCESS id=04c9b62ceefcf1c5dd8f6c823f8d4150cff4fef640c5bc1f4d3bd7d702b856a7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xpos.length : "glyphs.length " + /* M3-LEAF kind=FIELDACCESS id=5457e2be55ea87d92cac84e5638cf7fb9c3abb7c0588e2cbb8422d4b427e0255 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 glyphs.length + " != xpos.length " + /* M3-LEAF kind=FIELDACCESS id=959eb46b586e85169efccf3a7343868e04050e47923e4f9982831bfe2536ffe6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xpos.length;
            /* M3-LEAF kind=INVOCATION id=041f98aaaa2dcdf6f34114d64306eeed6c7bc278d6e50416825ef0890e2f3929 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=eadb629a5023b3ef3843085e2172aa9697a416390ffb7fb5f43068c9a27b08ee contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=ba96e6aafd7dfe525d3c8dc006bc426bceb711180627dfd22ed979a56843bbeb contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=d3cba534e8594c2871926845fb847e62472e8d715a85ff4d703d18019bfe974c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeFromPosH(glyphs, xpos, ypos, /* M3-LEAF kind=INVOCATION id=c8d564e0552d2b4dd755df9b5d96ff7ec973b8d889082a902710f1fa7a624fb0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(font));
            /* M3-LEAF kind=RETURN id=0630fd502534ca9dd45f6e19629274d22c6261f8fbd3854981142a021c8df7fa contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=23b7f34692f57e37c79f0fb158a1f1e14617756b96f8631254496b9cefbdfde1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=e5e1087f86db0aafbca2bf2f8d4cb2e59d5d11259f99d9917b2a32a5a5be4f41 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3TextBlob(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=92e5457cff3144121dc135bdbf950b1372f68f2fd7dc4ae4b44e1a9a7c1ab700 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    /**
     * Returns a TextBlob built from a single run of text with positions.
     * Returns null if glyphs is empty.
     *
     * @param glyphs  glyphs drawn
     * @param pos     array of positions, must contain values for all of the glyphs.
     * @param font    Font used for this run
     * @return        new TextBlob or null
     */
    /* M3-ATOM kind=METHOD id=1dbf581e95aee9776c86abf4cf7a473271c5ab27c90608f81689da1697c877fd contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY,ITERATION */
    public static M3TextBlob makeFromPos(short[] glyphs, M3Point[] pos, M3Font font) {
        /* M3-LEAF kind=TRY id=47e8f2dabf9d54f714eb83d842bc3fbe7e3ec73e5d6eb2b5a2563f24fc0ca77d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=ASSERTION id=12ca95fa994f884efde33857a5ec827a6ef1c84af0cf081303f74e7863481cb2 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert /* M3-LEAF kind=FIELDACCESS id=9769dac269a1585edf7269634ebc0d0d133e8084ca9a2a5df294492da8d05f36 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 glyphs.length == /* M3-LEAF kind=FIELDACCESS id=664dd73280f0a875c49904f0e1b89ca17c1df66c1c59c4c886f5291c991b7583 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pos.length : "glyphs.length " + /* M3-LEAF kind=FIELDACCESS id=b2fc61e1768455bc7b5993cefed344be3259334144b1a31bef5c5a9d72ad808d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 glyphs.length + " != pos.length " + /* M3-LEAF kind=FIELDACCESS id=fc73b8a510b2219760538934d252ad733453b671b1bf81ec354c53c62e64410e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pos.length;
            /* M3-LEAF kind=LOCAL_DECLARATION id=21b98a324b35e216f620354816963243100bb79811443a147f7b7d9e05437e92 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            float[] floatPos = new float[/* M3-LEAF kind=FIELDACCESS id=198726fa1164adc1eb2993dea6284495edcb4905a6e93d0c9d981ef22ec3aeba contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
pos.length * 2];
            /* M3-LEAF kind=ITERATION id=53feaba7c1e9fa47ee790c39a9a40ae463fd10f23ee4742d6a7d90313da29b33 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
            for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=bf8c23d431b030bbef0242467bbe163524ee4c31b24fa4702d637b83758c2891 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pos.length; ++i) {
                /* M3-LEAF kind=MUTATION id=cbf30c65ec67a9d09e6693badcbaf90ec8e35e64e3b8f76eb1c9900db8aed5f6 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                floatPos[i * 2]     = /* M3-LEAF kind=FIELDACCESS id=27fdeef709150a83c5d5bd7c5df4a59941dc254c4aed688c7984a6e93446d21b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pos[i]._x;
                /* M3-LEAF kind=MUTATION id=bf70735a8c4125f9113d93b5ef907bb4e39a779b11528ce3e50512d5950cf8f9 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                floatPos[i * 2 + 1] = /* M3-LEAF kind=FIELDACCESS id=a2372ebea26dc104b47ce30a59c2d212d2f88c852e3f44bd0ebe12d47d9945fe contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pos[i]._y;
            }
            /* M3-LEAF kind=INVOCATION id=84f1a64f77d5c24b9edfcac2be59fd170e1fe8e67fb2f41c6a910595f17188d3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=d99afce00a1acecb44bcaac4c156ce7443451ae67adbbfdda420cc1bfef30184 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=52c984fe93609045146ab2e23cce9e50c3b69e297d4f1807621525691d132698 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=1a99b5a11e58b02337bfbfafe5bd8f8923b19b9d2ce61bb6e6d261bf765bd75e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeFromPos(glyphs, floatPos, /* M3-LEAF kind=INVOCATION id=2706a6b89d896faf8161b8e54deadf433275bda3df97e99601524810dd9346ac contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(font));
            /* M3-LEAF kind=RETURN id=cead1b864237314b48063c941892744938e38a006fe2922426d291a27706bef2 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=6739f240907f8de2b6da5257e9ab558b37fcddc30fa8f3a016cc960b931bb648 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=8940730b7885fd901cc563052a9a797beaa2403d4903471364125eeb4ee13dff contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3TextBlob(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=899563938f6e3bd4b947b98e78dbbe9cf13954493ecc39712b0bd94cac744d0a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    /* M3-ATOM kind=METHOD id=875cd594ca54039ebcfc5c9330e3a2cee17ea6bd747f21d6d811b4c2fcacd986 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY,ITERATION */
    public static M3TextBlob makeFromRSXform(short[] glyphs, M3RSXform[] xform, M3Font font) {
        /* M3-LEAF kind=TRY id=08c3b6eca4aa9e1c1d0a8a3bc177e47fcc55229ea2cd0cfe563b6a1dfa97ef99 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=ASSERTION id=606f733c9e2c77abe51eeef61e5253ffc371e8a7eb234fc8ddeaf26980d1d15f contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert /* M3-LEAF kind=FIELDACCESS id=06d2c9ee51b38621fcdf068b32ce2d125e7eb6a0774a70ecfaf4d82ea26a8a52 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 glyphs.length == /* M3-LEAF kind=FIELDACCESS id=a02dd242c0f242ca22129987a7978a593701f9a99038bd13ddee66f0277c43ac contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform.length : "glyphs.length " + /* M3-LEAF kind=FIELDACCESS id=507b4adbeaf9a0c4185d05cdf1d09aed407bbe75376f09592bc289bcb9e70365 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 glyphs.length + " != xform.length " + /* M3-LEAF kind=FIELDACCESS id=a1e86da58b4e84b705fd3c14b113a5a98a1a4b7ee4975ee8bfe09e5acec0926b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform.length;
            /* M3-LEAF kind=LOCAL_DECLARATION id=afa3658c995734015e948f95f1fa34fb48df177ae342da40bf9a1301b7faaa9e contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            float[] floatXform = new float[/* M3-LEAF kind=FIELDACCESS id=2063bd503482d52b1c3dc6ae44daafc37950452098abb9d6cac19af475ed28a6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
xform.length * 4];
            /* M3-LEAF kind=ITERATION id=fcde0d3abb1c9aded151b776c34152e162623a1ec46c9e608cd7847cf1dc1246 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
            for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=42e23b65bb04abe18ac86e1547b3e328008ae3fdbaf0f7ca998cd717a7dc64e6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform.length; ++i) {
                /* M3-LEAF kind=MUTATION id=ffe76f23dff27efaac73bd3eae30e150a2569b9bd5178c864a70d934d5264fac contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                floatXform[i * 4]     = /* M3-LEAF kind=FIELDACCESS id=7c59f3ab53cd1300dbb1579c2e88f50b5bf7d911742a71e433978cad774fb9c9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform[i]._scos;
                /* M3-LEAF kind=MUTATION id=ec4ba6f751333f8c0a8d6758094ddcd6f63c83d62c4779f725953b8d87017341 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                floatXform[i * 4 + 1] = /* M3-LEAF kind=FIELDACCESS id=937c71e589490946bab3d072aa13d39539cfdf41a733f38da69847977aa5fdb5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform[i]._ssin;
                /* M3-LEAF kind=MUTATION id=b52183b2f886b7592d99ce0b6d33bef0af57ae5c7ed8deae385de72512589ff7 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                floatXform[i * 4 + 2] = /* M3-LEAF kind=FIELDACCESS id=4d87200f09e20aec42c16b15977eb8f5e5df42e11558f7cdebfdc4873a3a727a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform[i]._tx;
                /* M3-LEAF kind=MUTATION id=2992cc05ee71d9efe2db19f99b08c1f5c82d1908811115d981060316a3085dd2 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                floatXform[i * 4 + 3] = /* M3-LEAF kind=FIELDACCESS id=408218b673035a349afe37121bb19a07a059eb407a41666f5d163e26d69ddc16 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 xform[i]._ty;
            }
            /* M3-LEAF kind=INVOCATION id=fd3aa68155189464c43473e7ee57444d98ec3ed9b339fdf1a638d6c12fc719f0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=4b556792f33275da163e062414525d1ffb5e04effc982ecfd3efc13176cfda0d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=cbe17c7762c173156de8e091df06562bfa78f311e85f632848df6e867bd89496 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=f120716cbdb78ccf5900b528fb60a0c5077186f7b5871137906d0792404815b8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeFromRSXform(glyphs, floatXform, /* M3-LEAF kind=INVOCATION id=1d843aefd5322458c189ff09edf0542f7ee9e7b76a12ed8f3e9089ea990033ad contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(font));
            /* M3-LEAF kind=RETURN id=9b1f682bb0d0d7c184e0c5a5a2018e95c0865b2dc4c1985ebd6c3693cded447a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=b8b97b384534b53d8e9fe9c01e891daaa1939e5026f742f23f1405558632ce92 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=bde7257d86e50e1d3517a4e002f8cf6b964fb8b0b561ba631363596316ecebb2 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3TextBlob(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=12c6731f152bc9dd43ffbec0b088ca89ed5c25ad86c18963ae2c8954dd46f744 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(font);
        }
    }

    /* M3-ATOM kind=METHOD id=b86bab0a94e23b99baa9ae7f325e275af47de2a4847350d7cdf14c92cc5f24e7 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Data serializeToData() {
        /* M3-LEAF kind=TRY id=f31f94ffb4fb10a03b4535445ffe1be476f10d00f83de3b1b06eae6a8d03d20f contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=eefb0171b89a83b2a6e0b7386abc6787f63ef1924787fef719eb38016a34a7ca contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=491ba7f4c984fe87c997edafefbc1a133eed4598608f05b959e0a5f026ea6911 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=64d0ea86fa4f280d0e841425660cbcbeb601453c5a7cd5bf5efe67996e29c4af contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=117093fbdf966a88fced4a96ace3b4905d36806f7e288fc30f0a63002d872c05 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Data(/* M3-LEAF kind=INVOCATION id=03b25f13fc284b906e696984934821439d3611b4f1871030650f53cde327b2ba contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nSerializeToData(_ptr));
        } finally {
            /* M3-LEAF kind=INVOCATION id=bd954b8208e31e98841b69a8988e7b04359a192469beda1daa420168e7af1f71 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=7332e4c93cbccb7809609b8a3e0b78882e4e3494be4dd46a75165de62e9cf5f2 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public static M3TextBlob makeFromData(M3Data data) {
        /* M3-LEAF kind=TRY id=eda24d2b3a13085212a46dd77af3e3d57dded1068f61ad945fa04aa06c1afb12 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=f4992bf98a60f79bdfc6f0c287b667d90e97d0dfc4569563bab95811ae964f4a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=929bc1a2749ecc31e9785221c9e170b6ca89245e27e51fc41de91f96922bd8ba contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=a92c19bf88dc3c0b9370967a7e896d6a76e3d551436a67de36132426598d1293 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long ptr = /* M3-LEAF kind=INVOCATION id=e80f9e91481171a48e70bcbd653e0f51e88b6c620f222dec6859d32efb9ddd1d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nMakeFromData(/* M3-LEAF kind=INVOCATION id=0a0d8fd10232475ec6c4eed2586b68e5288d5dd385514f8c2254fe0cc2d7ce7b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
M3Native.getPtr(data));
            /* M3-LEAF kind=RETURN id=188c9016c86fa818017f8772138c52a499d15ede598e0aa59198597a9cf59f4f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=a3d0c82495a679b88015e26c5562b7113efc758238cc707eded93360275d8d41 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 ptr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=a166901b01f036437c9ee442c8136b2ed1b6faf0342239de4500e62bd6b8aaca contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3TextBlob(ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=db355f16cb6e64891b31eab1996bedf501b4b9b5a4902c89c7eaed06e0f23faf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(data);
        }
    }

    /**
     * @return  glyph indices for the whole blob
     */
    /* M3-ATOM kind=METHOD id=d99725c13fa3810ee2f7b15b577d9c4fd3480af1fefb7ff42c65fa9e29d2cc86 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public short[] getGlyphs() {
        /* M3-LEAF kind=TRY id=967005f2eea9525770d3a390e46dfc31da36939d77cf2bf7e4a452a980effa2a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=c0a31f0b5c738f32b3558f638ab96038ce47440747b2af294fed365c67a93566 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=eecc177227d77c153cb669fdf088f5d395cc0bac41270be86282ca99ba81a72e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=41fb2d9d4632d3aa90f38fdcea6a697260053f388b7202ca658e6aefa1d0ceba contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=9979d3f0a640271a0789fdbdbf446e9cd99ac7eb36dcba2b5cdb6eb5f8274316 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetGlyphs(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=15fcd0a2b7c719e54bc41fc042f4eb59b7d532d2d933adcaa9dd4ca3b6a3ac33 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * <p>Return result depends on how blob was constructed.</p>
     *
     * <ul><li>makeFromPosH returns 1 float per glyph (x pos)
     * <li>makeFromPos returns 2 floats per glyph (x, y pos)
     * <li>makeFromRSXform returns 4 floats per glyph
     * </ul>
     *
     * <p>Blobs constructed by TextBlobBuilderRunHandler/Shaper default handler have 2 floats per glyph.</p>
     *
     * @return  glyph positions for the blob if it was made with makeFromPos, null otherwise
     */
    /* M3-ATOM kind=METHOD id=bfda57063a7aac7528ce2ace0e68f4be208b9e812b96c969a5788641429dc2b1 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public float[] getPositions() {
        /* M3-LEAF kind=TRY id=b9d72a82f6f103fd5645c5a4b07f562d77ad7e2b931da988c8ba8be602b116ff contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=0397dc004e65f3e0ff59e3e8336ea4a67d803c5f4bddad63043c1fc4760ce22c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=8a16286e315379a57c0e2fb4b65494b6203c8f8cdffb8eef2bb20443f1c8a30f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=f623d5a4bd2b2fdb14cb89d4da009f796283a3b8740b009548fe5c54454bec2f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=e27d68ec2db8a0cede8a14acd9df0b62c7bff0eb7a717aabf0a8cf63c5e486b9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetPositions(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=c47e19cfbd830732b479f6ba925ad48f59364b7fcc4de709018b9ea6bf485f10 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  utf-16 offsets of clusters that start the glyph
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    /* M3-ATOM kind=METHOD id=329d9efd8c3e5df39327f5cb904f3bf18a42640ede3f13ff9862e929ac6c032a contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public int[] getClusters() {
        /* M3-LEAF kind=TRY id=54b6e16c9e6c022197a743c48b3565264e45bfd712fc3305efcc6474857e2d1c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=6a2129e80edfba080364e7025906c2e0b999a52bd769d86fcd38cfdcb016379c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=a4187ce812439ddc16a7b4f24bbae92890fd7138c974c9de74828c67e91c164a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=8be2d4599daca5a21f9aac85359c43b9e2e2bf547bb321b493e61ccd7b10e748 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            int[] res = /* M3-LEAF kind=INVOCATION id=b70d9ae0203b7ca9f8adbb70a851bf2d9c92f02c4bfd894e2224da5202e8c790 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetClusters(_ptr);
            /* M3-LEAF kind=BRANCH id=778bd878bb4962044051025c7882480a8e27e861facc257689deadffda28da6f contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (res == null)
                /* M3-LEAF kind=THROW id=a3fa1611c2a6771aac0e7be2a522138a00afc8a6a181301809065ae96b775472 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
                throw /* M3-LEAF kind=CONSTRUCTION id=6265ca62b4e0ad352eb362690c78e3b81b38b22b69e1acdb71aef451f07848d1 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException(/* M3-LEAF kind=EMPTY id=7634f94ee1d2a793bf90a9584426334bdbf3fcc3bb259bfd9698c892c01465ed contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=72fe9eb941c7b9039a3abe0613625f1e4f5941ab6eb2c5953d085223aa3a3c77 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return res;
        } finally {
            /* M3-LEAF kind=INVOCATION id=08ecebed6b4cc490064d527fca7de6bc48a5d5dd8701b5af891c53779405ec97 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  tight bounds around all the glyphs in the TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    /* M3-ATOM kind=METHOD id=6308974d19b5d7a7cd26dcaf070ba5681f4b840e7d6e4ebdb73c068fae9bc494 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public M3Rect getTightBounds() {
        /* M3-LEAF kind=TRY id=b6173e2ed5f3a9d4310597ad2969b4bee24c398a68f6654b89022456deb0c483 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=521cf58c5c87d7d5ac862c4e2f93529dc2d60b434183afc4227b1a2b4b05fee0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=8682c11d465e49a44b82ddcc59b187b94e559a763e9441bf86a7cc76f6afd5b3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=9fb47d5c51fc1442a2338460253d15672e17970c38542c9cb0f344579f0c99d0 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            M3Rect res = /* M3-LEAF kind=INVOCATION id=589651f518e2e3d573954dd129e223c56ec63a320d36c0754cd22b633e791cb8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetTightBounds(_ptr);
            /* M3-LEAF kind=BRANCH id=cdc5414a5accb21ad5c44b03c94d7527847fff15a969ebed54ccf9af1c04f0c6 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (res == null)
                /* M3-LEAF kind=THROW id=ec90be90502036c188ff90f331cff0a79b997ae57f814513a0dafc5390aa52f3 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
                throw /* M3-LEAF kind=CONSTRUCTION id=b08e28ef9599744826f915bde94446df91b0712c316e69c91eb99b3c0ece15da contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException(/* M3-LEAF kind=EMPTY id=77952eae6d520824bffcc3e7730c09cdf47a364aac1829807b013e752085a24a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=a6cd76300c462529082446744f3916cd25a578ea77b7e17e272845749e8004af contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return res;
        } finally {
            /* M3-LEAF kind=INVOCATION id=b5ecc632407ccf3306415fe254b56042b1671cfcccec554e6bdb9b39d25718e3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  tight bounds around all the glyphs in the TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    /* M3-ATOM kind=METHOD id=adf20f2cafc01bc3669dd9ad07f736f19cfdf52283ea4723fe9c370b9e07571f contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
   public M3Rect getBlockBounds() {
        /* M3-LEAF kind=TRY id=a03af4a33b4197756e37855e6373826b58f0f6eb667bb96de2af29a2b1af9fad contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=528a57b98c2e1918aaa516b7fe2e279505bc10393a2917c477e38d46277666fe contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=64890c7507758bb9212bd4177efdeb5d6f62bc0cc89e8b485c9917a722aaa821 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=7e51ff224bd337c5d1a2d7c963fedf5715ab7f0f5a249efd8a04bbb7bb5f65cc contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            M3Rect res = /* M3-LEAF kind=INVOCATION id=f0336bc2fe0114c1572e37b71a5327e9fe3eae6b49558fe6e02969d5f7bad9ca contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetBlockBounds(_ptr);
            /* M3-LEAF kind=BRANCH id=11976fc1c139aa0af58666d2814ab1b612c5e420b2c6da5baf03f08b8b65ce91 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (res == null)
                /* M3-LEAF kind=THROW id=f97fed9a17e764f873df70c4b27ad3f1765f5c0c08081a71b053a796b4261ed1 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
                throw /* M3-LEAF kind=CONSTRUCTION id=7e373eaf4c3846f8855cf9fe31d8b094fd2a390059132fcc7c3649d771e64a31 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException(/* M3-LEAF kind=EMPTY id=0f53442a5239ed09a7c92ebe13b2f4b4cafb414481eec4bc473a19a3247bdc8d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=81afdfd3d06d98f215602df39b3b06ffd759fc8f953a6b51fe8ba9684e52e962 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return res;
        } finally {
            /* M3-LEAF kind=INVOCATION id=ec4c440830b653b2b21d99f54e3104abccce51a4b1aa53dda8ca350282fce50d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  first baseline in TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    /* M3-ATOM kind=METHOD id=45a7d8126c81ec5ffea9c6a734bf74c34ea6743569b044e990a94d86075d59aa contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
   public Float getFirstBaseline() {
        /* M3-LEAF kind=TRY id=5badc9a741288c398fd6e84054102d5d0a8a692152e5e31ac931188ee1449841 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=d97a2e0a84c716da7b3d5444919c1c900e85921b520c3da8bf8108b866c48814 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=256d26b5160bba5a67a6ec14e41e431efd6e7dddeaef2c81cae03d4e8c5a069c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=3ba465783d69eab7322a577fc573ae1c5a23139793a76230e1aaff712ec5079f contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            Float res = /* M3-LEAF kind=INVOCATION id=c282ad77a0693402e4b968f2bad99f952ae066dc87b1031aa5583bde83e8c862 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetFirstBaseline(_ptr);
            /* M3-LEAF kind=BRANCH id=283cd599a73cfbdd3f0d85658a000ec3e23a1c56c57d690c3ede36f0d9adc2c6 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (res == null)
                /* M3-LEAF kind=THROW id=3895a4fc8ab68b5130bfd9c2e082b31d465a9399ab1cd3bc14582396e1846469 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
                throw /* M3-LEAF kind=CONSTRUCTION id=d2691e958b7f1f4a418947eec9cd591b5ff2f1360cd92120e89e5e0afb8b569a contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException(/* M3-LEAF kind=EMPTY id=37c42fedc3fca47649a507405c720b85792655144617811247435d6f37a6f078 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=f14c48b5691a7645343a550e4df8e8036ba6875614457779810a79a2fe155aa2 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return res;
        } finally {
            /* M3-LEAF kind=INVOCATION id=abc48d60d1af1500c9296dbfe08ba244960962ca295da7d7dd2dd8709a7499a1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

   /**
     * Only works on TextBlobs that come from TextBlobBuilderRunHandler/Shaper default handler.
     *
     * @return  last baseline in TextBlob
     * @throws  IllegalArgumentException if TextBlob doesn’t have this information
     */
    /* M3-ATOM kind=METHOD id=ab622d3f24ab123df0273ca364bc5c351b3a713b884cb857f2eb6230bad027e7 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
   public Float getLastBaseline() {
        /* M3-LEAF kind=TRY id=0af7a5145b1b3ff0177bb284fb4bc9080cdd850dc3d6c40da93639e98e1095e7 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=876f20cb159df4de55eaa03a1325a33b3efe41dfc73f78639d1361d33b544942 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=6a41b4355815395d81adc20485f4cc27c1c3c5623eb79ede74ab50011aeeeacb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=055f6fcb2430f167fad2ce7bdce99da0d9064d85026d1ff16353ba735cb0c5a5 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            Float res = /* M3-LEAF kind=INVOCATION id=4a9a49e2ca5f557cf097ae560e5065d8634bc07c7e67d23103d59c05e06c481b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetLastBaseline(_ptr);
            /* M3-LEAF kind=BRANCH id=c8c6c29a8cdb54b48f3819598b0b26738274e732a27320cc6fe804ba602e6483 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (res == null)
                /* M3-LEAF kind=THROW id=2432cf07044685a67cd90fae66d777c0896e08d763a131397f23ac659371465b contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
                throw /* M3-LEAF kind=CONSTRUCTION id=d6113a9fa586b9b64ae3d1df8319fceba67b809eac8b8a3995553ec3fbcc35c0 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException(/* M3-LEAF kind=EMPTY id=bb97e4c1e8e53a9f6fe625078249c6a2ea70d4c2b9486c178075257e5f404bfc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=f48c0ebbd49e0335232a76b93888c78a83c3e55dbe4a7c46589d667b1ddb0499 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return res;
        } finally {
            /* M3-LEAF kind=INVOCATION id=d1b94af84456f0aed888340d7082b75db241e2a00133b20a6b7348f1bc380851 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=TYPE id=b88502ee632411d978242e468ab950e39044f5b2be7ff390dfca011bc8ad8e52 contract=PUBLIC_API patterns=PLAIN_TYPE */
    public static class _FinalizerHolder {
        /* M3-LEAF kind=FIELD_DECLARATION id=695d9c8fcb28cdc14222cc89d0d1e8fe2a3c9df11a819f46f6267357636e135b contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
        public static final long PTR = _nGetFinalizer();
    }

    /* M3-ATOM kind=METHOD id=bfba88ecc70edfd3dc79328112dcb36a1d1c5f993e864f248d3ee59289cb1435 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long _nGetFinalizer();
    /* M3-ATOM kind=METHOD id=a51ed4bdfc772c8e7521dc057d1fbbee9a4d027bb299b9b46d5f4217d9ee09b8 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect _nBounds(long ptr);
    /* M3-ATOM kind=METHOD id=74be5f8ebaf3b29c8e2fb98864d79ff8c94841756d3faee46b78eaa5e336afb7 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int  _nGetUniqueId(long ptr);
    /* M3-ATOM kind=METHOD id=79e55f64f575b92ee7588e15f61a7f6b403437992e67ca35340e3d4708c29fdf contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native float[]  _nGetIntercepts(long ptr, float lower, float upper, long paintPtr);
    /* M3-ATOM kind=METHOD id=84ec79b583faf21426a669f1b233a827a51a69e066244d7637501588602740c7 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long _nMakeFromPosH(short[] glyphs, float[] xpos, float ypos, long fontPtr);
    /* M3-ATOM kind=METHOD id=9da9d5d0bf60af972e9f8705bc068892e4e43fe05cfb8f32748a8f8a755be2ac contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long _nMakeFromPos(short[] glyphs, float[] pos, long fontPtr);
    /* M3-ATOM kind=METHOD id=b9560ab6d0602c1a01b023d61a03399434a61e1fbed7171c73373c0345965edf contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long _nMakeFromRSXform(short[] glyphs, float[] xform, long fontPtr);
    /* M3-ATOM kind=METHOD id=b3066d94f7f6e73d1ab004e3b2c88dbf58bf7450ff2cac0ea2c032aecefcf687 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long _nSerializeToData(long ptr /*, SkSerialProcs */);
    /* M3-ATOM kind=METHOD id=66beaf76c6baa1cb6233943850a151efc3628601b643779f9fe0d7d57783a21d contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long _nMakeFromData(long dataPtr /*, SkDeserialProcs */);
    /* M3-ATOM kind=METHOD id=bc7eb584d2fa3068ccb56f09b2e41096df78c2e8fd9e55c355c4e201d66c33d1 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native short[] _nGetGlyphs(long ptr);
    /* M3-ATOM kind=METHOD id=b5ff2c24597ffd357a03c6ce1143e2239f74551fb7d0381e62943cc7e438ea85 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native float[] _nGetPositions(long ptr);
    /* M3-ATOM kind=METHOD id=d356099b4591290435cd56a3fd8b451b05c1ee8de07037a43035e70cbb180937 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int[] _nGetClusters(long ptr);
    /* M3-ATOM kind=METHOD id=1d84d6a0ce7828303c8a6b4be2da1fb842447eaaa1de1c7ff7378b1d77b10550 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect _nGetTightBounds(long ptr);
    /* M3-ATOM kind=METHOD id=07245b254ac19ee2f8af4ec1ca7a08cb70f3f47e7a9c189688430f0d17efb090 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Rect _nGetBlockBounds(long ptr);
    /* M3-ATOM kind=METHOD id=cd0097d56ee03aa969c8bfea512b71bd6b67466b80948f14a3330689a6f139b6 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native Float _nGetFirstBaseline(long ptr);
    /* M3-ATOM kind=METHOD id=54206231912012684cbff3d7753693e186e275b41725283a611b91f78559a5de contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native Float _nGetLastBaseline(long ptr);
}