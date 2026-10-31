package jdk.m3.graphics;

import jdk.m3.graphics.impl.*;
import jdk.m3.graphics.*;

/* M3-ATOM kind=TYPE id=aeacd1c1e69074ab6b868b5c53da6b1d913d440b3326f1fcfc2ecbb81a764910 contract=PUBLIC_API patterns=INHERITANCE */
public class M3Paint extends M3Managed {
    static { M3Library.staticLoad(); }

    /* M3-ATOM kind=TYPE id=98d87634264889384080f41ef6558cbd74b2dab0dfafc9ce4d8db858f295cfc6 contract=PUBLIC_API patterns=PLAIN_TYPE */
    public static class _FinalizerHolder {
        /* M3-LEAF kind=FIELD_DECLARATION id=5e1135269c56ae2fcb94bde4871e57147f298cebbb1d80b1690fa8468718be25 contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
        public static final long PTR = _nGetFinalizer();
    }
    /* M3-ATOM kind=METHOD id=18baabbf7e9d0d84c02acd1bf387d1275efb3c3c8a73813b8686328e23082aee contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint(long ptr, boolean managed) {
        /* M3-LEAF kind=INVOCATION id=db711af674b2570ac84d37fb3ea2952275df444dcf0129c09728ade076f20a81 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        super(ptr, /* M3-LEAF kind=FIELDACCESS id=f011cfc4949e91de0cc38ece19d9754b8718b82ad2158dd2871a1e10c30095b8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _FinalizerHolder.PTR, managed);
    }

    /**
     * Constructs SkPaint with default values.
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_empty_constructor">https://fiddle.skia.org/c/@Paint_empty_constructor</a>
    */
    /* M3-ATOM kind=METHOD id=88c0c650ed3c2dc7e5f60152af5b6363bf85228e79f1234323c821ece69f0522 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint() {
        /* M3-LEAF kind=INVOCATION id=14bac79b284b3c5f3f9c5a424e44c50de2662c808c7268ece748a774b8956010 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        super(/* M3-LEAF kind=INVOCATION id=0bf51df53cadd36ff452770dfa74bc78b31f15248ec3fa9d16c1ec425e6a18eb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMake(/* M3-LEAF kind=EMPTY id=d46f64d9c386f4f852ce4f70604c3c04c5933352409de538ccfd676b5796ff5d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=FIELDACCESS id=aeb38b2a11657b5ea464ec465d7bd5bdd79b089af2de0c7e2dca395ea1a35dbe contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _FinalizerHolder.PTR);
        /* M3-LEAF kind=INVOCATION id=12440bb3631024d65de3cd4aa8fe19ba7224af3da5d9eb30148c5094bad56ab3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=eff78ba71a231d3f54f9853147508ca73f6660358b4489882a0b7e0c069d436f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
    }

    /**
     * <p>Makes a shallow copy of Paint. PathEffect, Shader,
     * MaskFilter, ColorFilter, and ImageFilter are shared
     * between the original paint and the copy.</p>
     *
     * <p>The referenced objects PathEffect, Shader, MaskFilter, ColorFilter,
     * and ImageFilter cannot be modified after they are created.</p>
     *
     * @return  shallow copy of paint
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_copy_const_SkPaint">https://fiddle.skia.org/c/@Paint_copy_const_SkPaint</a>
     */
    /* M3-ATOM kind=METHOD id=8f9e23fdc66de7a5faa45688c912d3de40427958a2c45408d357b1ff48d38469 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint makeClone() {
        /* M3-LEAF kind=TRY id=275d966944e30ce9542d8348d9950c6c25b7da43afabadc22454cef1bdb5adf4 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=bf2a62a149805735ed7037b2b1faaec167321b86295d823bc01ace2b727529dd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=cf884bbef584fa141b3e3310cdf430fb84c9c55633e6ea2c4c08074336bc3c0f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=53905a8d2928a5cf031bc2abe13a51ab1a6ea3614801f3577d1830d2e1e06d6e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=6aaf4410b7e89eb96b28bfc5d91a78eaad8164957cd053bd6ca1f346df3a5ae4 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Paint(/* M3-LEAF kind=INVOCATION id=717f8fe285ebb17311f3ed88fa778a4ee6d4238e768a7ce4ea194315bec2f1eb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nMakeClone(_ptr), true);
        } finally {
            /* M3-LEAF kind=INVOCATION id=d3d294881647cf21fd511ffa7d417f86f014e98f0e523b321dfd368d951a569c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=854c56a2f3c0caf1c5123e4cfbde44694a00ad5f6c3b18bc412157f962803d50 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    @Override
    public boolean _nativeEquals(M3Native other) {
        /* M3-LEAF kind=TRY id=8d08b394d5db6891ce71b93cd65783502487589898c4b7818222efbecb787184 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=RETURN id=8c2304ad59f852a00a0c62796ddc8b1212a273da58d751a9f72b3a8e7319fc52 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=e404e65fd806307e1fda9e983936652a94da3eaa6aa520265bd054b5c52d53bf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nEquals(_ptr, /* M3-LEAF kind=INVOCATION id=8e9c85013ac5ef3c1177f2d94cd98644bd51cf84847d33556f0b0d123aa4f0c2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(other));
        } finally {
            /* M3-LEAF kind=INVOCATION id=8bf8b6ac547955d1136dfd902e4a70b87a902589a111bdfffcae8c8adb7488d6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=0275ff16a10b71dd4791b93da39bdb8255cc5b5000ae1507da4c80d41afafc6c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(other);
        }
    }

    /**
     * Sets all Paint contents to their initial values. This is equivalent to replacing
     * Paint with the result of Paint().
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_reset">https://fiddle.skia.org/c/@Paint_reset</a>
     */
    /* M3-ATOM kind=METHOD id=8c153d0c6ce4ef3bdeaae77f04438d922bf0b2c943d6f1c2c29108174e5f3848 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint reset() {
        /* M3-LEAF kind=INVOCATION id=72847be57d05f3b0059defee0ef737c174c8d50673ae2c1008fd089273a083f8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=327b06ecd756178090bac9ef9d6f6ebe39a865df8fa51d9f254ef1507e1aef5c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=1020dfeac12ab6b96fe87413d08f2e14f00ecdf2973fad20c566f49f78caba0b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nReset(_ptr);
        /* M3-LEAF kind=RETURN id=902d8de669dd39bafe86bf7c87a4024097e6ef988245b30198cf99efb764f940 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Returns true if pixels on the active edges of Path may be drawn with partial transparency.
     *
     * @return  antialiasing state
     */
    /* M3-ATOM kind=METHOD id=e03fb9c3c3d2367b346b4bb9f425492809b5bac3d299ab5a9fb99ba6a20e5146 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isAntiAlias() {
        /* M3-LEAF kind=TRY id=257f1070eede043fd890befc0ad7c914a7e76137b5c252d32bb9fdd273ff16ac contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=b6050d9a08a0f2e6283db46b6579708c904fef5d37e44f5bb83a3b16139aa3ed contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=743b8a5d33dbeddf8adb0b46d2e58f0f83ffa4341757892725cfcc411e728c55 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=17157b36853cc2beabdcf26dabe34d5c67479be5f0f2643b397aa378a87f0981 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=385192a183fc0e37d3365e730eef72f24875f90a1309b636086b17f6c3d9ea98 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsAntiAlias(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=0b262168145df869a157bdd59d5c62f557a241981c6c9b23e85d79f84b46584f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Requests, but does not require, that edge pixels draw opaque or with partial transparency.
     *
     * @param value  setting for antialiasing
     */
    /* M3-ATOM kind=METHOD id=fdeca76f75804ffe905d93ff59c67f1a43440f5b8f7e327f428b06fa8612bca6 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setAntiAlias(boolean value) {
        /* M3-LEAF kind=INVOCATION id=596f68eece99de6e8adf3e48bd198955c0add4070bce6b9d1c4e4fdbbe4eb251 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c49214bb9180795652fb992a9024ad83a2452c0c86efcb2ccc9bb1cfd1a76856 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=538a5fff4930863a33baa26f897fd522b4cf56191e66ddcfd27a081c258ae897 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetAntiAlias(_ptr, value);
        /* M3-LEAF kind=RETURN id=e515d1d0aaa05e901465fed8a4d0a87a379507fd13a62e6c50a65ead154047a6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * @return  true if color error may be distributed to smooth color transition.
     */
    /* M3-ATOM kind=METHOD id=0897715c85d31c300594743166bd94e2a71d32d18f71b3c170716f6400f53066 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean isDither() {
        /* M3-LEAF kind=TRY id=c5f7ab231304dd06d78a71edf46177fe7eb57159a5f656a4ca410aae21e3f9a4 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=03a25f5dd5866898253b26f07932d681edbcae18193be888589b6f290ccf39c2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=e93755c360134e7da8839dcf35199f4d260f2e845b4d9a2e1b6fa430cc689cbb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=b833d151549b6ae21ea9d4c313458567fe4e6f09a259467b0d949680cec789f6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=2b61ca56cd7fd8ad1d63dfaca86b1b10a4fa5c964d42c86bb3ea570416dd3da2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nIsDither(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=382c174e012e43077a665f37460ca27eb743a1eccd5277ddcd4d58f7817cfd64 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Requests, but does not require, to distribute color error.
     *
     * @param value  setting for ditering
     * @return       this
     */
    /* M3-ATOM kind=METHOD id=a5d7bfb6f931bf44212a56dbd24fec7d070dd9cb79febb616f1eaf4b0f41712d contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setDither(boolean value) {
        /* M3-LEAF kind=INVOCATION id=144e428d3bccfbd924a66e2b4e740e4a0ad7703eedbff3aa3bb1300ae04eb6e9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=bc78f37a5649570e0a38eeef75dc9d2dede6dbb12acf6897df241a379085e3d8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=b6436c4097883532063827fb5cc901f9a532dbe182ba2c08264cad1cb1c6e2bd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetDither(_ptr, value);
        /* M3-LEAF kind=RETURN id=a8e47ecc882ee4a524baf7a9ddb3d40642ff072773e7979d9e5b1415174da917 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * @return  whether the geometry is filled, stroked, or filled and stroked.
     */
    /* M3-ATOM kind=METHOD id=b9034707061b3c2fcd9d6ae632e291f6922991d7471f738e6e06ab8a5bef6344 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3PaintMode getMode() {
        /* M3-LEAF kind=TRY id=c5401c1c109e2de268e911e8b7a1615e97d904afd3dda24d2a40354f0835d419 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=807a4c637d766750a1a0a5b8c46d21748dcb531d8d2dd88f28e6dbb4d58627c0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=ccb25c2b367f63fd254d78464e3f1a4b8202387f0da4fa9eea6871fe64505714 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=408cdc1609e389ebb73533046fd0faa0a73c948b908a8353ebb2a8a86e64b67e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=FIELDACCESS id=fadf2c4f28bf44b83961f2a5ec3d4e19c916875ee76b15065a7e21036d0f5179 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PaintMode._values[/* M3-LEAF kind=INVOCATION id=24cc82c5f03669e8cc4a96f4d44ac08b36327381ab2a6e72bf5b433a8873c009 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetMode(_ptr)];
        } finally {
            /* M3-LEAF kind=INVOCATION id=b66bb4d627755d992aa7c2c9f78ccf6ba8b4142f2c258d4baf92417dd5961c0a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets whether the geometry is filled, stroked, or filled and stroked.
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStyle">https://fiddle.skia.org/c/@Paint_setStyle</a>
     * @see <a href="https://fiddle.skia.org/c/@Stroke_Width">https://fiddle.skia.org/c/@Stroke_Width</a>
     */
    /* M3-ATOM kind=METHOD id=bffa3ac5e845dfac57f429b4523341221a48d808d1c0b158be6715b0dc429d77 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setMode(M3PaintMode style) {
        /* M3-LEAF kind=ASSERTION id=845ed2d9fa10befe642e81f2450216fe8ad9bdeecad98f2743b4d27ac9d3d67e contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert style != null : "Paint::setMode expected style != null";
        /* M3-LEAF kind=INVOCATION id=028e3f47bee9138ffc7f526e82234d08e882f2cb88579c17a9acc0f2b1c95fc2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=54e80f87adf74b9aad5e3a0b3808a633590de53a14e67d2cf72fef2b7b6766d4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=d5706d02feaa36a0882f6904ece88b50d7176dbf3c1c65454af60311f7627328 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetMode(_ptr, /* M3-LEAF kind=INVOCATION id=981332dbda9d7709b1465c0ddeccb2f8cd5134510907a11ec0ab025a16f3e6ec contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 style.ordinal(/* M3-LEAF kind=EMPTY id=055eab5fed04ae86c619ec5767d7b2ad28b1ea08573e5b653acc95fdfd49cb90 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=RETURN id=750c704890f021b358d3f57f22985bc1c65812e88724311efd170f246e422d70 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Set paint's mode to STROKE if true, or FILL if false.
     *
     * @param value  stroke or fill
     * @return       this
     */
    /* M3-ATOM kind=METHOD id=31d096002ba0acfab69067b721f190f79c18dbd7f57ad5385b07c7497aaed7d9 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setStroke(boolean value) {
        /* M3-LEAF kind=RETURN id=09133e4fb7afff723331b9b1964a13555d201c93d75ae7c052f39aa9c600019f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=33926650a5d53ccfa97e4f8a24f3542b35771ef067387af0efc36c5daa3f5315 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 setMode(/* M3-LEAF kind=TERNARY id=2d8c4971fc1f83d537b1c3c2e1cb05f82aa268a1bbdb0fe055b65e1ae67baef1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
value ? /* M3-LEAF kind=FIELDACCESS id=38b13e716df84f420979d95c1dad88097364abc58400ea454ec6fece50ca2110 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PaintMode.STROKE : /* M3-LEAF kind=FIELDACCESS id=5b989af8aad38822bb176f78b0b2ff9a33276a8dc60f83f8009a567a9a2e4702 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PaintMode.FILL);
    }

    /**
     * Retrieves alpha and RGB, unpremultiplied, packed into 32 bits.
     * Use helpers {@link Color#getA(int)}, {@link Color#getR(int)}, {@link Color#getG(int)}, and {@link Color#getB(int)} to extract
     * a color component.
     *
     * @return  unpremultiplied ARGB
     */
    /* M3-ATOM kind=METHOD id=6217a519d96b25ae262d2d7517e86bc3f3ea172183fc572f0ab3823b372379c1 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public int getColor() {
        /* M3-LEAF kind=TRY id=2c9a358264fb4e64a6f28f00a5f296c73868bc23ab3cdceb9fffd45cb34cc7e0 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=1912b31bc7e1706e6a3dbecd309e8154eb9be277bcaca595ab0b591030fda6e6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=db9f095801fa24493b4b1f3d95142561ad3f14a10dfea351f1dfe38125f9dee9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=9adc85bca47352a3e0f7f2b85a327ad8d90c0fd5fff6f5dfd2ba61b093a72060 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=53db7c0206968596603b277ee2405737b4ffc277d4973f15bae92e54bf30cbf1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetColor(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=fe7c1fe1978b6733dd4d9ed7da5b3c68ff2632557ed09589d32365ec598d8559 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Retrieves alpha and RGB, unpremultiplied, as four floating point values. RGB are
     * extended sRGB values (sRGB gamut, and encoded with the sRGB transfer function).
     *
     * @return  unpremultiplied RGBA
     */
    /* M3-ATOM kind=METHOD id=8dcaab5678973084aae801f89aab95b725fd07e0da8000e4765fbb41804b382d contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Color4f getColor4f() {
        /* M3-LEAF kind=TRY id=3c50eea03556ff514b7acb31756f6590f998c3d4eca92c5784f49bf062301ec1 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=e0d1364b8e8019daa20768dd499757984bad4c108a2a96e111c6e2e98c30d149 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=ab63aa594c5e4ddc2e213c03fb71c25157d72876914a04846ce4ba123b6d0d29 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=c463a13b002467d685b81995892732c7549572c23eceaf7f63f34c71e8ff8376 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=4f1e292213e90c11631b655933629aff8f3dc7bb7d180e8d78241748fe52add2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetColor4f(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=9bfe7c5c2b0082fa3b91a478197a8a573ba5c5b2defe98900d453f0cb058a956 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is a 32-bit value,
     * unpremultiplied, packing 8-bit components for alpha, red, blue, and green.
     *
     * @param color  unpremultiplied ARGB
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setColor">https://fiddle.skia.org/c/@Paint_setColor</a>
     */
    /* M3-ATOM kind=METHOD id=c4bba14939e7d085c7d8ef0705886ccd4789c8da87d6ce86103ff87b29d2de9d contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setColor(int color) {
        /* M3-LEAF kind=INVOCATION id=ac62d13665bc5ad56b2364c474cda91d044a11a668e797f9fb9e6aa36c309b60 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f57b41a6f69ced1bf7fbcf503ddeade2988c6ed1f5d30eafa39493662150019c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=8f8d518d3505ff8216c3612b5ba9bb050326f04eabb4bc689beeb8d930dadb97 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetColor(_ptr, color);
        /* M3-LEAF kind=RETURN id=5b3c48cb386c61acb9e24b43ec07291e518c13484d13ed2103909cf74fcf49f5 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, unpremultiplied. The color values are interpreted as being in sRGB.
     *
     * @param color       unpremultiplied RGBA
     * @return            this
     */
    /* M3-ATOM kind=METHOD id=c93453fce7d6c75b51f2573fbc3901c1f0963106b6a6b69e55707449c468d055 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setColor4f(M3Color4f color) {
        /* M3-LEAF kind=RETURN id=da587c0493d9cc9559c9a801e322020f280591106174a02818b867f9ce541b6a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=0ba08288e6bb1b3afa7140b9b1a6d90893618c9f4a5c54d06e5673328744ab9e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 setColor4f(color, null);
    }

    /**
     * Sets alpha and RGB used when stroking and filling. The color is four floating
     * point values, unpremultiplied. The color values are interpreted as being in
     * the colorSpace. If colorSpace is nullptr, then color is assumed to be in the
     * sRGB color space.
     *
     * @param color       unpremultiplied RGBA
     * @param colorSpace  SkColorSpace describing the encoding of color
     * @return            this
     */
    /* M3-ATOM kind=METHOD id=22743d4e570c68f307fa54ccc44049fea3bd765c8e17e8cccc9407102f30d8b3 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setColor4f(M3Color4f color, M3ColorSpace colorSpace) {
        /* M3-LEAF kind=TRY id=a74a631ea497cf9751b591a93ff303e57b95f9f1113b73443ae9b73973bb7a7a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=ASSERTION id=21c29d31ee4af9494404548f83438687dd84e624dc94599dc77707676da6146e contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert color != null : "Paint::setColor4f expected color != null";
            /* M3-LEAF kind=INVOCATION id=4b14e23535a2a73cf8ca2bfdf9323eef22fdc5b03650f56514245117863d93e8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=7bc097e6b2cf20f06909201a62bbf6b47a61eda2dbcb4980c661e2b46779a693 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=006487c41390dbff001951ee447fadf1e690cc0ffa88ac6e31bf7751cbabc7c3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetColor4f(_ptr, /* M3-LEAF kind=INVOCATION id=092c91a30b6a010744736a9e9d208bd10d46ab653e7b3fdbe5b4c0cc3054f827 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 color.getR(/* M3-LEAF kind=EMPTY id=adb7cf69852023ce5bfea8b887fdbe53003cd3f9aa366459a40376ce130958bb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=7ad04dc0b859be82da1b2e831cf7eee16a9805b2026bbb66b7239274b9d9ba71 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 color.getG(/* M3-LEAF kind=EMPTY id=aae4bf7b0bf2d22a34d0b946bacd55de094ec098e1843b6ad065ade0ee4fa64e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=26b5e602f0e2b9f0ab2abc4cd736afd60658ce552b7f06ebb696a72338401d6c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 color.getB(/* M3-LEAF kind=EMPTY id=65d97fca179b5c1addf1a7302be319b20b5d0362aa2a0276a1e60d7be5b9fed0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=5ccfdde3b929df56e20a91f255763f95706c964fe58a2839165790af71085478 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 color.getA(/* M3-LEAF kind=EMPTY id=3f64ae17f97c93f8e973db2b53ff5a63bd9ee72fc0fda63f482759e9e2ec2c2d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=5f3c319bd433a5c8cdc3efd0665053b0034ca9cd55104a9a651995c76ccd1c2f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(colorSpace));
            /* M3-LEAF kind=RETURN id=cfe6a9c16576a101a04b03b347e5c48743f63d185e638d3511f8c09cdca2d6a5 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=0cd7395adf4d9ccecb7540c02e355ec1e6cd2f42faa0c4f943e23c9893d3d41d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(colorSpace);
        }
    }

    /**
     * Retrieves alpha from the color used when stroking and filling.
     *
     * @return  alpha ranging from 0f, fully transparent, to 1f, fully opaque
     */
    /* M3-ATOM kind=METHOD id=801d6acbb64b6f325028700e0ea3cb6e05965813aadadad1023a0c1f92399c7e contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public float getAlphaf() {
        /* M3-LEAF kind=RETURN id=09e728cb46dc03fe40aeba47e0cb79c65e656286380cb14b120c1d0691e37d99 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=8fd03fc2ba541157f0d02030f521fe6cf2539a1b6f978fc93db77104db4f9bf4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 /* M3-LEAF kind=INVOCATION id=4ae3e17cc586b0245ec1bbaf99a746cba7ad71144cc73a14534f593e99ff3626 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 getColor4f(/* M3-LEAF kind=EMPTY id=66f8c3a21275eb230ad9210bb0efda7f6b57bf96121603e3dfcab46d7e98905c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
).getA(/* M3-LEAF kind=EMPTY id=32f47b4bde3c6d951783ca76849d80f87ffa611d9b0b68cc9eb8d587a887cfde contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
    }

    /**
     * Retrieves alpha from the color used when stroking and filling.
     *
     * @return  alpha ranging from 0, fully transparent, to 255, fully opaque
     */
    /* M3-ATOM kind=METHOD id=5a12c2e7f4beee83193fb2cfb2b790caf8238259ba4f90cabb6d4a9e875bf8bb contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public int getAlpha() {
        /* M3-LEAF kind=RETURN id=f840ce2ce6e72539b4b10bcc2f5a91b02fa68b7170b2ce40e35ff415f1a87da8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=16a250a7c42d3517b56b2aa0b1f8357a47adee27b57ed2e4f0bd4fdba083f634 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.round(/* M3-LEAF kind=INVOCATION id=43862185ad90cce3abbb3672f28e765d45d63d234022320689281b3cebab27d4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
getAlphaf(/* M3-LEAF kind=EMPTY id=340fa17b960bfc399da9489e72128f6cda7070af9e9cdbe555f4f0369735fc37 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
) * 255f);
    }

    /**
     * <p>Replaces alpha, leaving RGB unchanged. An out of range value triggers
     * an assert in the debug build. a is a value from 0f to 1f.</p>
     *
     * <p>a set to zero makes color fully transparent; a set to 1.0 makes color
     * fully opaque.</p>
     *
     * @param a  alpha component of color
     * @return   this
     */
    /* M3-ATOM kind=METHOD id=43efb1c218d298b64d759981f702b6ec2a98e453cc728f2cbb436e8bd4e67584 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setAlphaf(float a) {
        /* M3-LEAF kind=RETURN id=2711ce5b3d862642ab39326d409917e9725da4011efc3e68e7320a1cabcb68ea contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=6ac9629b0aaff8a3a60e3f583741c579f63c9a026297bd3e9c51094f5ba31e40 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 setColor4f(/* M3-LEAF kind=INVOCATION id=93ec2515f78951b0d629c8481c70a1409d3529e888df965693d53880444fad98 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=INVOCATION id=cbb5ae9f360994c53618e2dc01d36b6758487a30ea7be8cffce15da780535fd4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
getColor4f(/* M3-LEAF kind=EMPTY id=d08a32a9e7f42a0dcab53d3eab64322f5c4a504f5ce81818c7ae26d613da589a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
).withA(a));
    }

    /**
     * <p>Replaces alpha, leaving RGB unchanged. An out of range value triggers
     * an assert in the debug build. a is a value from 0 to 255.</p>
     *
     * <p>a set to zero makes color fully transparent; a set to 255 makes color
     * fully opaque.</p>
     *
     * @param a  alpha component of color
     * @return   this
     */
    /* M3-ATOM kind=METHOD id=29f9767349dedd320a390aceaa69667ddb3ab87ebde5e1f805428ff913cee48c contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setAlpha(int a) {
        /* M3-LEAF kind=RETURN id=7f7a31bb2eab5a60c585cda09ccae00123d2b52d7e0f9b54ff6e670a447015c8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=60d311c80fbace794777940863325ea6bc38083d4950bc75bab5e4a409ef3eac contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 setAlphaf(a / 255f);
    }

    /**
     * Sets color used when drawing solid fills. The color components range from 0 to 255.
     * The color is unpremultiplied; alpha sets the transparency independent of RGB.
     *
     * @param a  amount of alpha, from fully transparent (0) to fully opaque (255)
     * @param r  amount of red, from no red (0) to full red (255)
     * @param g  amount of green, from no green (0) to full green (255)
     * @param b  amount of blue, from no blue (0) to full blue (255)
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setARGB">https://fiddle.skia.org/c/@Paint_setARGB</a>
     */
    /* M3-ATOM kind=METHOD id=b7b42e118a124692058421a5700cc8c4dedf0040c373223a1bd0ce08dbcc43d0 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setARGB(int a, int r, int g, int b) {
        /* M3-LEAF kind=INVOCATION id=b65b292a3afab554b9d70734c93b1103266248f19c8a60bd67479144adfa074b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=a7a74aae24ad84cbd70fca76db0b55a0c82544074730f936d3ee059ceadf8aec contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=d0d76a48fbb60d849594616cb384220ed2037e08286683c5cfc05d505c7aa051 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetColor4f(_ptr, r / 255f, g / 255f, b / 255f, a / 255f, 0);
        /* M3-LEAF kind=RETURN id=be3266f06d058ab7e9c406bea6961063ccae67c07e87af5e9202627be803147e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Returns the thickness of the pen used by Paint to outline the shape.
     *
     * @return  zero for hairline, greater than zero for pen thickness
     */
    /* M3-ATOM kind=METHOD id=563d6f03cfd5ca231a8e1bb775957ff5f8c6d18448e399fe251badb115b12d91 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public float getStrokeWidth() {
        /* M3-LEAF kind=TRY id=5d2a1647ff0af9a3c6098cbf54067963264f7aca66db4bb65eec6cbb6d26050e contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=0cbeb07545801b44a56860c0314d3282ecdf575aef5a7db2153b124467921890 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=3747b661cca3ebc96b22cf275b354a207b3373c47bbe052da7432684184d02c6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=75813a16936cc769c7efb129e2264035e6e68a6cc752db777410c0e09d4f1c47 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=b62a15890e62a02b675ee1c257e03f41cc6adf3f58a225198d2c83efdb5ccaad contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetStrokeWidth(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=8dbaa500278bd137544d0763f5c5decd1bffa860a7d6f1ce2bc7621369ddc470 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the thickness of the pen used by the paint to outline the shape.
     * A stroke-width of zero is treated as "hairline" width. Hairlines are always exactly one
     * pixel wide in device space (their thickness does not change as the canvas is scaled).
     * Negative stroke-widths are invalid; setting a negative width will have no effect.
     *
     * @param width  zero thickness for hairline; greater than zero for pen thickness
     *
     * @see <a href="https://fiddle.skia.org/c/@Miter_Limit">https://fiddle.skia.org/c/@Miter_Limit</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeWidth">https://fiddle.skia.org/c/@Paint_setStrokeWidth</a>
     */
    /* M3-ATOM kind=METHOD id=46aec6a9ae8afd1af7dd34696129789ff3f8cf1f26797f88b5fdafbb15d19011 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setStrokeWidth(float width) {
        /* M3-LEAF kind=INVOCATION id=87fb25531c3cc9036676f60d1814e4a43d5256e174bc39d6d4e220d5708c26d2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=f07dcb81a76cedcbf58e9da2900cd5330a36541e0dad05b09082e7372b8bd89a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=64c767aa5f7edbd408fbe00a6547c84e3db829a95c0542dab50fe1596a983991 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetStrokeWidth(_ptr, width);
        /* M3-LEAF kind=RETURN id=ddd0867e2e0802b306b35af9a73cc06572f40c44d779dd732021138a7b1c36c3 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Returns the limit at which a sharp corner is drawn beveled.
     *
     * @return  zero and greater miter limit
     */
    /* M3-ATOM kind=METHOD id=ecfedf752cfe09506245fc1b881fd2fd11deb59da57a64b5615186d77f5c2104 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public float getStrokeMiter() {
        /* M3-LEAF kind=TRY id=20d9ed48fb1ea3ac9d60de0c69da9db16f21c27b816018934c783b53d7084e5a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=15dd774f846d250a5537f936604b2eeaca260b003dc618c469b5f731c83ba264 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=a9b5d69158f02ad387332bf0db8e553c908adba82dcb6380c9de89485087ab88 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=cdf81d0519858bf0f33ff9c0656280a6d1d1240a93658f618fd2983c287f82c6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=9b370b6a435447b13d8b4f9da391ed817ad01d01108b804871f35b77ce5e3ee9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetStrokeMiter(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=cb455b024fd4c8bca317f03d31de01a921d262e39f9a5e3e856f4015f8f32fbb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the limit at which a sharp corner is drawn beveled.
     * Valid values are zero and greater.
     * Has no effect if miter is less than zero.
     *
     * @param miter  zero and greater miter limit
     * @return       this
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeMiter">https://fiddle.skia.org/c/@Paint_setStrokeMiter</a>
     */
    /* M3-ATOM kind=METHOD id=26ec249443d4bd8e887a59343ec268650afd096abbce8080cb4f828a955405b7 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setStrokeMiter(float miter) {
        /* M3-LEAF kind=INVOCATION id=0fbf2daa94b0357e8d0fd34d800e89e8bbe6ecb929f9114859397fde691aa879 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=4ac9c22a1e22243eb91d3f5d64ef184007eac7cb7af11cd3cae8fbaa051582b2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=0573b86d51ac028b1cbc5713f1da61059d962ebbc058bc1bdde8af225e1325a9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetStrokeMiter(_ptr, miter);
        /* M3-LEAF kind=RETURN id=24649eec135521ab5e4d787208f4c2342c9c525de202d519b4cc998af28a4b1e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * @return  the geometry drawn at the beginning and end of strokes.
     */
    /* M3-ATOM kind=METHOD id=193ba9f68b88642520cd0183854334229f3cbea9314f357b534384780ddb9932 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3PaintStrokeCap getStrokeCap() {
        /* M3-LEAF kind=TRY id=cda60b2a278e0db5d088b5276efec23535b57c0db263a01836103449aaebcd43 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=38bf1116f10d85576ed7d8be2714c6d898b2c98c614791fc6f43a05c914343c8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=b3984a4390328df9a10641364ec61dbc5f70a924c93e6b4372e9ffaab182b49c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=0f2b791a765040b735e2555330d11211ae89245bdcaf452e13842d095cc6775a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=FIELDACCESS id=5cc952948822eecdca5ee9b15b9ef7adb7a8bc11e813e54159cdd6c4837f7e2d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PaintStrokeCap._values[/* M3-LEAF kind=INVOCATION id=af5296b6fce8f1dec6107ab89ba1f951e1d606a6083c38553d45af7a5936cfac contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetStrokeCap(_ptr)];
        } finally {
            /* M3-LEAF kind=INVOCATION id=7d222f3e601b8f461deac6fbd44b20d52726480b9da3eb10d385e382fb05feef contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the geometry drawn at the beginning and end of strokes.
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeCap_a">https://fiddle.skia.org/c/@Paint_setStrokeCap_a</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeCap_b">https://fiddle.skia.org/c/@Paint_setStrokeCap_b</a>
     */
    /* M3-ATOM kind=METHOD id=95a0d7a001208e983ac989e9e2b13176c95961a9e49d16d0dba11c70351de70d contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setStrokeCap(M3PaintStrokeCap cap) {
        /* M3-LEAF kind=ASSERTION id=4bc6f6716168b275d970d809c32a01e85c92041ca2766d7d74c7c49853b9b57d contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert cap != null : "Paint::setStrokeCap expected cap != null";
        /* M3-LEAF kind=INVOCATION id=9fa81d5e49e8f552692c3ab0e3a50649787116f114228b18fbc9a326e111e59e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=7a03b9b627bb9667743fc88aa820547d2dcb4613b117143cc4e946267e84f776 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=d440fbd5ad0487a3eb68a5d0b24c0c2dc074156f308080ceaed131c291b5cc52 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetStrokeCap(_ptr, /* M3-LEAF kind=INVOCATION id=7d1dec34c5278e43082566fa24e0d364ec463fc78dddc649f5f2e190cf08bddb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 cap.ordinal(/* M3-LEAF kind=EMPTY id=f2dc420d4da76de85b81f7937c538164bec8f0620383df7d710d124c08748e7b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=RETURN id=720ad33c3b554c01b3210d7ef9c25decd25c0cce5ab2615681e3598d70af0d33 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * @return  the geometry drawn at the corners of strokes.
     */
    /* M3-ATOM kind=METHOD id=ab73e4cc5a35de3060b91aee3b9c51378b5c9be9fb87638314ca549ff7a59020 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3PaintStrokeJoin getStrokeJoin() {
        /* M3-LEAF kind=TRY id=8cdd0d016376941cf58fb2d384a22b5033fba378b0be5a329b1178f313422f11 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=ffc98a3877cd21a96f2539bc51681edd92c3f494cd57bded598fe60dc9ed993f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=7b2bd2fd88935c38420fe0a4b2d4176d86cfd2566d7c5dda1e824d9e276d43e1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=9df725a43e2218ca13e99f3ab2a744901bb90f5e232abc55b0374ff1d8ace925 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=FIELDACCESS id=0ea28c62395b1da716bff39d8c4a5a8df9627ab32fb81f98dff726cee82aa816 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3PaintStrokeJoin._values[/* M3-LEAF kind=INVOCATION id=7916c27c69e2314ebbfdcd4bf0e83db17cb1e211efc757016328378af949d872 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetStrokeJoin(_ptr)];
        } finally {
            /* M3-LEAF kind=INVOCATION id=228ca371e6c69c5290cea06272dec033bbee73494017f8e06a9f05e4c146029d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets the geometry drawn at the corners of strokes.
     *
     * @return  this
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_setStrokeJoin">https://fiddle.skia.org/c/@Paint_setStrokeJoin</a>
     */
    /* M3-ATOM kind=METHOD id=3572637108fbac1fc36873a9288cb1bc99f1a85260b8b6adecf1f7b715e4416a contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setStrokeJoin(M3PaintStrokeJoin join) {
        /* M3-LEAF kind=ASSERTION id=e4e80bbac7a8a210f611d186e641e885ce25c9edab14d4239964fd857d703fc0 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert join != null : "Paint::setStrokeJoin expected join != null";
        /* M3-LEAF kind=INVOCATION id=83efa921d3089c88e9898b5ebcfd47d6f640e6061dd81526a2cb02ab64ec6168 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=7023ea97a623165b3c62cbea5c9299190368208a729b57e145dcc0fccbfae05e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=9a9568134f18b5edba6c291b19f2ab98e8c2e79f611794f9f79869fd4601703b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetStrokeJoin(_ptr, /* M3-LEAF kind=INVOCATION id=7392c641f87febd8da1b589cc468e9e22f330da5a15284d2b10ea93143bca09c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 join.ordinal(/* M3-LEAF kind=EMPTY id=e6ad0874fccec211ca8cb6cb55265ba42909c76f2e3f9261b6d970f2184158b3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=RETURN id=ff8aec9660092dd381c47a85c267e8303a511fdff1f0689ad9a411e26de12f95 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * Returns the filled equivalent of the stroked path.
     *
     * @param src       Path read to create a filled version
     * @return          resulting Path
     */
    /* M3-ATOM kind=METHOD id=23975bb269089ccd13834b0152e6d65be13e498968cd50d6f2d453aa33644ca4 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Path getFillPath(M3Path src) {
        /* M3-LEAF kind=RETURN id=b6bf5c98c176f0e87725921d4442ab7e4572c370afec54ceb704b0ce720c8c9b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=671019f01f604949203cdcbb62719e4c2e7abc0a3cb8787b4ec32d7954700e79 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 getFillPath(src, null, 1);
    }

    /**
     * Returns the filled equivalent of the stroked path.
     *
     * @param src       Path read to create a filled version
     * @param cull      Optional limit passed to PathEffect
     * @param resScale  if &gt; 1, increase precision, else if (0 &lt; resScale &lt; 1) reduce precision
     *                  to favor speed and size
     * @return          resulting Path
     */
    /* M3-ATOM kind=METHOD id=8cb200dd58415d685943d4e382c6f547cee925f5d1abc4a7816b790316644392 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public M3Path getFillPath(M3Path src, M3Rect cull, float resScale) {
        /* M3-LEAF kind=TRY id=2c2eafc292ddc0a1a139916533b6d8538ab33fc15585c84486f7320c97539ff5 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=ASSERTION id=fbeb5c695637eeefed8340e866b90fe8d5e0095d4a88544b4ec362000de7b1f4 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
            assert src != null : "Paint::getFillPath expected src != null";
            /* M3-LEAF kind=INVOCATION id=4fbebe99ae4c138117b1bb20a2d0e4368f7aaf409365807004dd8fe3512d82d2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=9aec6475cd3301f7303d46f77dc4eeedf4711a9aeb48f5e8c974732f90443c73 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=BRANCH id=9c04b3c7faf6e4b183192972be7deb0d5b44cd54569c0588884461b2c2df79d0 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (cull == null)
                /* M3-LEAF kind=RETURN id=6945a325a0a5af8c5db3bac3dc07bf5f5b22f74c2a4854c2cc60de7cb3b2c31d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=d22e5b43acc25fd10ecbfd285c6c5090ce641c6ae2963c0da30607828752b889 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=d3e36ab7cc973e742885f804b1ed7413b2914ed82e357061178b842c229c534c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetFillPath(_ptr, /* M3-LEAF kind=INVOCATION id=8f73df6f3df4f53f1ce15f9d87047d335362eb520e6f914f75f74bb61dc0e71d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(src), resScale));
            else
                /* M3-LEAF kind=RETURN id=831a5b86ebda434da70c4d08859622be2b74913ed1aac898673cbfe325d2c945 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=274d15863573479ac637d839e480b390ee43a1d04bf339bf77e17387a957da60 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Path(/* M3-LEAF kind=INVOCATION id=06d2550e6953d11f05091f4636fc9d6bd6218c8ada286da4a356c4611d1ab8fe contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetFillPathCull(_ptr, /* M3-LEAF kind=INVOCATION id=21b05cbc0da67cd8f2d1781872dc679867ec14ca92488f107dfdacc0d804802f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(src), /* M3-LEAF kind=FIELDACCESS id=148be832a820766bd74c4b217f215a59dcb9c448dd964c3164188f09fddfee9f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 cull._left, /* M3-LEAF kind=FIELDACCESS id=e75de147633a2174eca216d555af129293094c0e099f84a16fc1ace9108c868a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 cull._top, /* M3-LEAF kind=FIELDACCESS id=a32e72ab1d01a7d04f638ee44c13a847c2436070cf17c5bc70ed2587452abb1d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 cull._right, /* M3-LEAF kind=FIELDACCESS id=71a045fcb747e85f8bc593b43959ffa815490b189c308425816e7a82ad867380 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 cull._bottom, resScale));
        } finally {
            /* M3-LEAF kind=INVOCATION id=50b999187bdd1aa543812033acabbd8374c4d374a0db6876fd8dcf0e1e11502f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
            /* M3-LEAF kind=INVOCATION id=6f44fef36929adba9df117e4d75fec5ace68c10e3afa5fa561bf0fe820fbcc2a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(src);
        }
    }

    /**
     * @return  {@link Shader} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refShader">https://fiddle.skia.org/c/@Paint_refShader</a>
     */
    /* M3-ATOM kind=METHOD id=57c8ccb0e6825562826318d27f31a63b254f1d5abba8dc6d644e9862cdce7a84 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Shader getShader() {
        /* M3-LEAF kind=TRY id=84ffeab5c401ce2377cbc226447a1a764ecc47f6f2cfceb230cfb8d67ab9b5ae contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=1683188f4ce7896c828b8f5e502ac320e474937642c12c8a7c68413b21c7a2ee contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=1cbe35fda7d19c41fee377d249879142ec16ee95536ffefd4f12456b8837a2bc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=c622e976cc7527efd9b5a39577a3a5a05fc0f29d300209d2500a81c0ac3e6f72 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long shaderPtr = /* M3-LEAF kind=INVOCATION id=84bc1c42fb40d89401eb16cce86e3d63a3bbc6702964566fc7004c4cae255a9d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetShader(_ptr);
            /* M3-LEAF kind=RETURN id=cedb1457f6806559c294c838a35c16840f4e1e6be92f378aed8bd17ded02bb55 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=3be8a48102d24de70840e2d00b313529196705327a13909b038c7043f84342f2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 shaderPtr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=3d477bda3e6b6a22d5d84aadcea355c2bfccc7443a9c409f522126ff1bf8b4d7 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Shader(shaderPtr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=a33ad32f7ad92da74c29ff9a8ec56d3c311e63fcd5426cca0e3a3592fa343d51 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param shader  how geometry is filled with color; if null, color is used instead
     *
     * @see <a href="https://fiddle.skia.org/c/@Color_Filter_Methods">https://fiddle.skia.org/c/@Color_Filter_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setShader">https://fiddle.skia.org/c/@Paint_setShader</a>
     */
    /* M3-ATOM kind=METHOD id=f6765bd089504996171832fa0c4120bbf5841dd57dd7aa74a49ba719a30ed7e6 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setShader(M3Shader shader) {
        /* M3-LEAF kind=TRY id=e2b4662a24646cd95524d0fcab69d86f7e796cbc9f4dc4b3c8d240f0ef1c4972 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=7256334e9de4eb0d45dd750b9e7aafcc37e3418eb719e42fc7fd6f9e2da6eea2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c6abfff70a6139839754f40f87754cf245cc13a8e492a59ea7e195a15a0fb95a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=9e5aaa9273761fc05b7681fb63ffeebf5640c7aa03ce7a94edb40806160c385f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetShader(_ptr, /* M3-LEAF kind=INVOCATION id=b209c34ebbd22d487351b836b72a6511e662efac9cd264667321fcfca9c518fa contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(shader));
            /* M3-LEAF kind=RETURN id=6c099e79fb408053776c9fc57562419609e0fb3b4bb21d6a6da2e95fbf3e8d4f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=1bbfd59380b0b435003ef6dc3f79bf578c72111fdfc86da82dd222869905589e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(shader);
        }
    }

    /**
     * @return  {@link ColorFilter} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refColorFilter">https://fiddle.skia.org/c/@Paint_refColorFilter</a>
     */
    /* M3-ATOM kind=METHOD id=ebdb0e157aa02c121ec5e3c41fc7888f4c6c24b69a6c7c7790b56f3bdabefb72 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3ColorFilter getColorFilter() {
        /* M3-LEAF kind=TRY id=01640abd0d4b89d45c14c44fc4ceaa9eef858ee2071f5fc5c00cecddea554bfe contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=0c68860f1e8f79f4803c6e950cbaeabebb44e89906b8f8dbb48ace0eb833fe09 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=9757d92cc84fd587ebc4bae3adcdd209c6169ba679fe53d311622158786d770f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=8973d6dddf6e414e99f32c5cc4c883f8b764923b03863202e44d770b1959e1b2 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long colorFilterPtr = /* M3-LEAF kind=INVOCATION id=6cfe3aeaf1a6ddfd2c5af1e34c29c0afd27a2ade122cb0b3c7108a56b51191b0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetColorFilter(_ptr);
            /* M3-LEAF kind=RETURN id=e9c689eca7e8cb68da30f529a8e9c4e17dcbc4413320d5b78188d9172f942fa5 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=b9496fbd58792b9d1292a277fffedacd452bf6a688c67969891a1a30f4249f59 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 colorFilterPtr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=8276688afcfcf363e14b2e72f924ea4ef56fc9745385115d129c57cf37f89d49 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3ColorFilter(colorFilterPtr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=40c5a7e8326258b98e83d0a9a512590a1c0e9294f2299cff9fbc5a759bc002fe contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param colorFilter {@link ColorFilter} to apply to subsequent draw
     *
     * @see <a href="https://fiddle.skia.org/c/@Blend_Mode_Methods">https://fiddle.skia.org/c/@Blend_Mode_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setColorFilter">https://fiddle.skia.org/c/@Paint_setColorFilter</a>
     */
    /* M3-ATOM kind=METHOD id=a27424320860abcb6f710b9f606b791ade6ed7ea833cda92d25827102d4a5d54 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setColorFilter(M3ColorFilter colorFilter) {
        /* M3-LEAF kind=TRY id=154f5af889ef4a2d9bc941f797f659d0868b182d505f894edc5b858b2ff58d1a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=78d76868ad3da44621b1941163760a987b236f1eac8316c48bd3d658a398cb1d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=c2659c1eb20e9cdac122451b2b4a6e498bca917a0b7c65735881fe04f76b8a69 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=31cd614493c6bb20dfc134c303a8a42c007648227c7127346a0d9760a15cceaf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetColorFilter(_ptr, /* M3-LEAF kind=INVOCATION id=756a84b6cb36afd00ef464683a8647b81a628663bf02b2d1945ae2cffc1aa45e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(colorFilter));
            /* M3-LEAF kind=RETURN id=6c99b5e38b7698de718b81585615b8c0e88862a4e9718b6b77f31069bc3e45bf contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=763b64aa261ff9bf23e7651140d9ddffe1891f15ffd309e08406592b6af6342f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(colorFilter);
        }
    }

    /**
     * Returns optional Blender.
     *
     * @return  Blender if previously set, null otherwise
     */
    /* M3-ATOM kind=METHOD id=0e994feb7c33fa62ee5198a3e4b16cc93f7feb8733fd23490d2340624ccd4b54 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Blender getBlender() {
        /* M3-LEAF kind=TRY id=fc5ffdaeeede3fda30dcae04ea8c887d7355bc7e987c5b9203136b7f628740f0 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=39596594cebdb6ba07d9617bca6ee2e503b8483aa59bf6cf5533a3d10380c094 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=19e953bcf2cde3a02908869c96259dc1f23f79c66c6857109b98a0e071f613e4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=ae708e54e56a31ace45692b847da952786e4b0c0a688a2a1c0ff7104e6cfccc0 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long blenderPtr = /* M3-LEAF kind=INVOCATION id=ed17537155365bd8ec95d77e1d0f205bfa39e436bb193f1d8a376ac546161546 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetBlender(_ptr);
            /* M3-LEAF kind=RETURN id=5d31e9301aefec693b9941287a9214b53c85a7b4b67ce92f75af9919882cff6b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=e2fa80941a5c3947d918f3f7e478bada505f508afaa3ce2c2fe9ba14b27ad1e7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 blenderPtr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=1a27bf59200594a4810a86e348fb1481e88c7878396093e5fb24f0377a8f30c7 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Blender(blenderPtr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=e77814e8a4acc06dcadfac6a81832286fb1172ec5d86a71dac1d09cac521b269 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * Sets Blender to blender, decreasing reference count of the previous Blender.
     *
     * @param blender  Blender to apply to Paint
     * @return         this Paint object
     */
    /* M3-ATOM kind=METHOD id=40d79e63b44ba8d7ca7b6b9e82c7d234ea781ba446e7ac784c830d0a826161f6 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setBlender(M3Blender blender) {
        /* M3-LEAF kind=TRY id=b11a62947fc62ea84c09e38389158c5c24537e834095c9e528c8ac9946a2b2b8 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=f0085685802cb402818788e6af88f7051755e1cb5422e4e0cc3f5f47469a2f14 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=8a6d63ee61c3a8080307e6d6b6a751d01e725d4ba78ba3f84b4938f4a815595b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=897bb8bcc3d4b57458f0b16d15e61379e85361ec3ee8f1ccd93f6e92150501c4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetBlender(_ptr, /* M3-LEAF kind=INVOCATION id=09970df042c3b3a3d1465576cfdf024f8d7d6f3d5b752b1b50d15e9bc0f3bed6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(blender));
            /* M3-LEAF kind=RETURN id=e95a010ceef13c50f5e7df592e76df6ce3c27a64e850c445e21939722cd79384 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=0cf4e8262a6e30f4c053d6c5f227032d3dcf916b9438f94757c0d94b8a697225 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(blender);
        }
    }

    /**
     * Returns BlendMode. By default, returns {@link BlendMode#SRC_OVER}.
     *
     * @return  mode used to combine source color with destination color
     */
    /* M3-ATOM kind=METHOD id=f113abab3b54fd908579e08ee99872249082c98980f8eabb32ba933e61ac9c22 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3BlendMode getBlendMode() {
        /* M3-LEAF kind=TRY id=46e758aa073afc47a487290a4bbdec00aa38853d75775efb6a63924802394d5a contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=346138ccb87c7254d85b38e30400720c29b2ffed76abcb5e251cca44970ac2ba contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=5f6667575adf6a5bea395f40e1f5b5d307cb527e491c19fd5144cab881c41d17 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=73b5593d2bb2e717a49f02eb0c8c4a948a5795d58ae8dd0a5080e7c8f160bdd1 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=FIELDACCESS id=f4d9ad2f9002d7e6515c31aabec4aeeca0a54c204a03d4d3b3a37bd69d1e043b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3BlendMode._values[/* M3-LEAF kind=INVOCATION id=18305ff54a53a248c46132d758c8818ac3c1c3050e9741db371b868812d44966 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
_nGetBlendMode(_ptr)];
        } finally {
            /* M3-LEAF kind=INVOCATION id=776742c3afa925aab81ce70cf06c413db445a3bba5d61eb9c13126f23d2a4e29 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @return  true if BlendMode is BlendMode.SRC_OVER, the default.
     */
    /* M3-ATOM kind=METHOD id=0d0b9723dd66dbecf2a1942efc55ca4fa11c61fe4b6e3005a8a54a2d57779144 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean isSrcOver() {
        /* M3-LEAF kind=RETURN id=3f0e405b2714e9883249f518309fda21f5ddbd12d6e55fb84be5f74da35f7832 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=1dd1e042a090330c70814a7ef245584afd3003dd3a46323a4f534ddc8647c33f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 getBlendMode(/* M3-LEAF kind=EMPTY id=7b1a582b82d2f63de669013f432d5d259914e770bea4b2ca6287a6f58faf28de contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
) == /* M3-LEAF kind=FIELDACCESS id=16e64c27d8b37826872b3e17f89420105fa64fc044964ce78047dfb2d0f1abfe contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 M3BlendMode.SRC_OVER;
    }

    /**
     * Sets SkBlendMode to mode. Does not check for valid input.
     *
     * @param mode  BlendMode used to combine source color and destination
     * @return      this
     */
    /* M3-ATOM kind=METHOD id=49384c177f81b28c814776d6c0576d58d2ed0e24c3079f15df61f87beebea44f contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Paint setBlendMode(M3BlendMode mode) {
        /* M3-LEAF kind=ASSERTION id=f29c29245899b70afd34a058760327e91ca23968ab0c859624e9b740087f4414 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert mode != null : "Paint::setBlendMode expected mode != null";
        /* M3-LEAF kind=INVOCATION id=ff0ec0692d2888282a0ee077a1b44af793f5710507d3324a0129c8ef8eb82c85 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=dd666d5af379fbdde59e3ee201e708ed65b0b3b13b6677a7ee69fda997736d33 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=INVOCATION id=773623fbc5f5bfc3fcffaaa68083ec1953f4b1c11d69e5e472ce5dfc19177122 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        _nSetBlendMode(_ptr, /* M3-LEAF kind=INVOCATION id=38f8cf174624d78e3289c86d02ec4302fb4d23b9d36117eda77d47a0623a7b8b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 mode.ordinal(/* M3-LEAF kind=EMPTY id=a0bc45d2fc397a18cd2187877ba4b886886c8c859e4f4d837f1ed934f2e63468 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=RETURN id=cef7db8dccc7d740ab0d67f2685b2e45f3d2ebcc06282735c06f9dc90ef7f7fd contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return this;
    }

    /**
     * @return  {@link PathEffect} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refPathEffect">https://fiddle.skia.org/c/@Paint_refPathEffect</a>
     */
    /* M3-ATOM kind=METHOD id=8859ed532b50934ddf385d71c724984422ebe2cc6998b0fa16bb313eb3d4bc18 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3PathEffect getPathEffect() {
        /* M3-LEAF kind=TRY id=05090b7934c0d7377ed7f7ceb8b8e47c2248a9cfe1f1adb83522522570e9750d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=546d0fd25cc893c321b9292e24f0ae37469cbbe4764e33fedf4417863ae0e50f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=97ebc949c3098f110457bf9da4c4aef5c03d5ee904cb88ce988c83ca1e6e2544 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=37f4fc05dac86884d76c1a2f7918830de57ba594c76c589b732bce8defb00fbd contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long pathEffectPtr = /* M3-LEAF kind=INVOCATION id=32c6c06e2b588c47cd87b81bb84769eda07c91ae1c5c29ab35ad04fcd12b38da contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetPathEffect(_ptr);
            /* M3-LEAF kind=RETURN id=d9d13043e7f33bc24c73ce33bdf69cbab9de7cc0368bdac43dc3b44e897b7299 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=1deec95154316f4c63fa0d3522cfd45b0448f8ca33cfc9e916c7c30e25991a18 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 pathEffectPtr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=680c2c0c4430f97c1f188bcf79fb9a03502a98b8dbf6629367655447c3d0267d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3PathEffect(pathEffectPtr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=b09fc083546465698440bc6b2ad2e3c4382e7c6977224ee70a65defa30eebe22 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param p  replace {@link Path} with a modification when drawn
     *
     * @see <a href="https://fiddle.skia.org/c/@Mask_Filter_Methods">https://fiddle.skia.org/c/@Mask_Filter_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setPathEffect">https://fiddle.skia.org/c/@Paint_setPathEffect</a>
     */
    /* M3-ATOM kind=METHOD id=f76614422c0c2ece833add2767e66560721126e978c422ae6c608ba647f5e63d contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setPathEffect(M3PathEffect p) {
        /* M3-LEAF kind=TRY id=876f6c2e708c8e817f7a1e951483a9a1ff038e6bd6942e7e90d3d3bde26a5b65 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=16b17f75c37e678a6970edf27ba675be76e824f0693b44bfef0be171152ac011 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=b31e91d98531a11960f2850d9dcf1962644a0553709ee6cd5498569d893489c8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=ca6c7f4aa8aa6b8aaac7ab805faa4b94e6effcaaca5d42dd57e1ffd80acdda5e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetPathEffect(_ptr, /* M3-LEAF kind=INVOCATION id=7f8e96dc7ab14051aaf3a1685bd7f47bfb771ef8c654a29a6096772d11f31287 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(p));
            /* M3-LEAF kind=RETURN id=6d5453e7abb10b7e9c5b0b065f42bd656d4c837c3675931e1be50935a7c2d8b3 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=2473e0eead4b1d904e0146ea896e953645d023f71863219da6d8c2376fa26e2c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(p);
        }
    }

    /**
     * @return  {@link MaskFilter} if previously set, null otherwise
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refMaskFilter">https://fiddle.skia.org/c/@Paint_refMaskFilter</a>
     */
    /* M3-ATOM kind=METHOD id=25f965b7c4bf79c4c1db39dfd849dadc3b10025e501ace810282444bf7157b26 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3MaskFilter getMaskFilter() {
        /* M3-LEAF kind=TRY id=5fa3061e075adcf7c6a76bc921d5e9e08b484b89ac0bb82300c0668c7db310bd contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=2677a2a38d5121b9db4a4adeb4f16eb0c44ab50993ac0ab7c81a553824fca7e4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=da2299e4f2fe5e5b6fbb28389c307fa5cfa1e58d314a50311498f7d320da7c53 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=3d9a3945a82b536795346b7eaee81b5738ac117615f4663179128473bf38216e contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long maskFilterPtr = /* M3-LEAF kind=INVOCATION id=32f21f08141a2ada10e4f09f384c0f706080213987e82f9278a1905399d28a83 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetMaskFilter(_ptr);
            /* M3-LEAF kind=RETURN id=23d4f9c27915f5eaa8a446233276fda2602ac71d19d43ab870f2a276c251cd98 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=60376a6d59c7e187652a44f2e63f709519f4bf72681ceb69c70da63acc3d2d26 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 maskFilterPtr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=f810752531faf74226fbac6f34be03421eb4c804fe91db95bb0e54841118c1bf contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3MaskFilter(maskFilterPtr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=88e4e5e1207a1b896b02802b6bbc54c84031044cfe3c028ee0db14adb2619803 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param maskFilter  modifies clipping mask generated from drawn geometry
     * @return            this
     *
     * @see  <a href="https://fiddle.skia.org/c/@Paint_setMaskFilter">https://fiddle.skia.org/c/@Paint_setMaskFilter</a>
     * @see  <a href="https://fiddle.skia.org/c/@Typeface_Methods">https://fiddle.skia.org/c/@Typeface_Methods</a>
     */
    /* M3-ATOM kind=METHOD id=26fbdcbad6f8b6c03bf6b9d5961bd56ec9f9247dc6c23cf2827d885807e284f7 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setMaskFilter(M3MaskFilter maskFilter) {
        /* M3-LEAF kind=TRY id=facbcb708b5a63884df10ce270b879409c524e19244aa99ef3586ea3376ff0bb contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=2b3433adea4fe002acff39831d074adb31c51a07fffa521984e1935c3d7f5b4b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=a33bd7eb4000569c18a53a8bb08065bb225ddc226a930d49452de86ca7ee8a09 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=ee5b947c0e1279bdc5dd3e548e1d5842751a9711ab185883c30bf6c15aa3e78a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetMaskFilter(_ptr, /* M3-LEAF kind=INVOCATION id=e286347c8d6843d7f45b6368d2ec5039e7a1f762238c97891e74d3201c030157 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(maskFilter));
            /* M3-LEAF kind=RETURN id=f7525dd2ba29118bf9e278cc1c088870388bf2a17b39774cb7702ab61170f519 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=bf89708548bad6a794a4615c0102e2db1b8d0be370ddd213c3471b32230237bb contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(maskFilter);
        }
    }

    /**
     * @return  {@link ImageFilter} or null
     * @see     <a href="https://fiddle.skia.org/c/@Paint_refImageFilter">https://fiddle.skia.org/c/@Paint_refImageFilter</a>
     */
    /* M3-ATOM kind=METHOD id=e22c5bb34bb50b2b0081fae6ca101268d03f065d7f5f956a8ae47b7ad9072636 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3ImageFilter getImageFilter() {
        /* M3-LEAF kind=TRY id=7248f3a46d219437d38bf5bb8b93a02fdf860ca31c231e732273d3a447c70a58 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=b31561b602499c165fd7583e915dbf957824f06cf98ac3793dc9ad73740f0166 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=924aa5881f33af166a5422673b55cd55a930046be0dfb871537b319ab4b6a60b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=LOCAL_DECLARATION id=844870109a44576847d50a40779ea54e0cb393604a9bfd311f87a7e56c5909ca contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            long imageFilterPtr = /* M3-LEAF kind=INVOCATION id=b4ca48f3929eb18f4e3be6e8ef0ee3992ca4054d5af5eee394c1b03514ace6f0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nGetImageFilter(_ptr);
            /* M3-LEAF kind=RETURN id=75cc7454d7fea82bbe878e64adfdfec7b8870fb341d0910dfcd37d064984fb65 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=TERNARY id=67b8f7064e04a3edab6215a7ea4e2ee06e25de43c71a90961908fba3d44dcb65 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 imageFilterPtr == 0 ? null : /* M3-LEAF kind=CONSTRUCTION id=1c6f6db7abeff7062727650830e011151a7a24ad99699bf41b0d212bdea8d3b9 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3ImageFilter(imageFilterPtr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=89a74397f8edcec18b8329188249f6be1698e001219f86d8234152aa04d4b8b6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /**
     * @param imageFilter  how SkImage is sampled when transformed
     *
     * @see <a href="https://fiddle.skia.org/c/@Draw_Looper_Methods">https://fiddle.skia.org/c/@Draw_Looper_Methods</a>
     * @see <a href="https://fiddle.skia.org/c/@Paint_setImageFilter">https://fiddle.skia.org/c/@Paint_setImageFilter</a>
     */
    /* M3-ATOM kind=METHOD id=26afb67f8c14bd4d9d9a3cdbddaf4119a1524ff36600900e7815a669610f7839 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public M3Paint setImageFilter(M3ImageFilter imageFilter) {
        /* M3-LEAF kind=TRY id=4adad500de1b77953604e94d799d1b23d489a880a5ab82896c1bca2f4447bca5 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=3b9c5618f745ad6d15f9e25997530c2f8adea3f0da27eb274c8f76c99c826f23 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=d8ea1e9d6ed354376a284d0954cafae678d12defd64256f7b2480eede5f4dc41 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=INVOCATION id=31c78ea6251f143812d158c8682686af9e3a9cbffebb3da394f17c82c1303625 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            _nSetImageFilter(_ptr, /* M3-LEAF kind=INVOCATION id=827fbd20444581e5e91a7de1042363118c69c87850fd01c5dd373c8e6f83a3f3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Native.getPtr(imageFilter));
            /* M3-LEAF kind=RETURN id=446ac2430675130791adfcb4de7e17445083470a8c553de711d69db9a6e69815 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return this;
        } finally {
            /* M3-LEAF kind=INVOCATION id=379ea36ba4ab6c84f86b74e949e8ff4a76568877e743b08c163b909cc9173af6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(imageFilter);
        }
    }

    /**
     * <p>Returns true if Paint prevents all drawing;
     * otherwise, the Paint may or may not allow drawing.</p>
     *
     * <p>Returns true if, for example, BlendMode combined with alpha computes a
     * new alpha of zero.</p>
     *
     * @return  true if Paint prevents all drawing
     *
     * @see <a href="https://fiddle.skia.org/c/@Paint_nothingToDraw">https://fiddle.skia.org/c/@Paint_nothingToDraw</a>
     */
    /* M3-ATOM kind=METHOD id=0893fc00394d4bce44274790307e04e28884c9b6c9f1b0dc754da74862573080 contract=PUBLIC_API patterns=EXCEPTION_BOUNDARY */
    public boolean hasNothingToDraw() {
        /* M3-LEAF kind=TRY id=5c1f22699e40cfb40dafaec12e98167be40cefb786477435f3f4c7f3f768fc9d contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        try {
            /* M3-LEAF kind=INVOCATION id=f364cb99694efc74397aa687bd7e1c7342369e5a5757811ae956fde8eaeb1f23 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3Stats.onNativeCall(/* M3-LEAF kind=EMPTY id=a610bf7f92af5f669bde2d64f6f6e69a8ef30a559cc9c450881cb47bec9be55d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
            /* M3-LEAF kind=RETURN id=0b6b610f2b7687be97c94e75a2b52fa2ac9cc37708534ed2650be82214c5cecb contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=INVOCATION id=e5146edfbf88fda0977b5fc84cfd008c0f66f95c7ceff95b9f444dda47ef5ca6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 _nHasNothingToDraw(_ptr);
        } finally {
            /* M3-LEAF kind=INVOCATION id=770d8d6c428e79b4ccac00823fb438169dd230245fcb1b4dfa5ccfe936f93f17 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
            M3ReferenceUtil.reachabilityFence(this);
        }
    }

    /* M3-ATOM kind=METHOD id=677aed86eab646f7c6e7443fd80257461668d895909a40be3325c0cf6e4b0f47 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetFinalizer();
    /* M3-ATOM kind=METHOD id=19c08e95581a84f3c88e0c415b37fff6ea07375c320d65f155e483fc578cc555 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nMake();
    /* M3-ATOM kind=METHOD id=abd86a5c65ab7f74e873d320832106087c2f57bb461666ece967610bd9d2e995 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nMakeClone(long ptr);
    /* M3-ATOM kind=METHOD id=acfe2f6d2938f06e0f4c66dd455f95405dfc83f0976e285f99acb3b69e207cbd contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nEquals(long ptr, long otherPtr);
    /* M3-ATOM kind=METHOD id=9bd3507ce03a99ee5d0658320df91b35e21d124d12a94734b01bc87e734e5db7 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void _nReset(long ptr);
    /* M3-ATOM kind=METHOD id=4f8f95561c91bd28a47aa71abfb4c2f8e871dd5a7141273194c1a736cff66ce2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsAntiAlias(long ptr);
    /* M3-ATOM kind=METHOD id=0dbc1499aeb13b0ff51d69bc1f688067c465ccc56067d1f9de853fa8802d254a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetAntiAlias(long ptr, boolean value);
    /* M3-ATOM kind=METHOD id=0f036133c9a347604b12f4b9277f8ea56ea71b0d42399a72169da61f417a3268 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nIsDither(long ptr);
    /* M3-ATOM kind=METHOD id=01a20568667d4cb7fe99885bf2dd46f02c52ba81106e69b77111f7ca5018c5b4 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetDither(long ptr, boolean value);
    /* M3-ATOM kind=METHOD id=dfff3faff09e42cd965580dd8d7c898c41a0e52c7be0a93487ada6d0ee1cd993 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int   _nGetMode(long ptr);
    /* M3-ATOM kind=METHOD id=5aea907bf59939a7ad0da3d9a31909081fc0457d8c6dc73c33e94f277bf5ffb1 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetMode(long ptr, int value);
    /* M3-ATOM kind=METHOD id=d526c06a632f12fefc5a4f942ac03c96061cfea2de43cd9c4b42951bf1a4e335 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int   _nGetColor(long ptr);
    /* M3-ATOM kind=METHOD id=26165a3340e1d578764a3dbb12e9045355c68c814c74e20a7427834797c0d924 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native M3Color4f _nGetColor4f(long ptr);
    /* M3-ATOM kind=METHOD id=911610575972f9f65a1a06d3d1be82eb901cfae080c8c9f182078d97eda1f484 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetColor(long ptr, int argb);
    /* M3-ATOM kind=METHOD id=c55e49dbcf53fb722fed295f053fed617cb5e2649c1c9ae81b4eb697af022fa7 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetColor4f(long ptr, float r, float g, float b, float a, long colorSpacePtr);
    /* M3-ATOM kind=METHOD id=ea89e1009fb4dfdfafbf03a3d06ad2a059bac5cd5a698f27efeb3d13d47b4f1f contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native float _nGetStrokeWidth(long ptr);
    /* M3-ATOM kind=METHOD id=c1c9c6788d7293575c1801cc860ba4319c15d9faf78b8ad0983cb2d2b0eea3b6 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetStrokeWidth(long ptr, float value);
    /* M3-ATOM kind=METHOD id=efd4781e1517e1f1c356e391448942a3d1b578c32c8228030173760a79d78c2a contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native float _nGetStrokeMiter(long ptr);
    /* M3-ATOM kind=METHOD id=3e3f184d275833bf976a71933248866fc610d588a6b40d2f69c1e81ee2f1b7e2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetStrokeMiter(long ptr, float value);
    /* M3-ATOM kind=METHOD id=5dd38ee137e340229389b7beca03056c7d91a63191ebe9e09fca187c3e3b0e41 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int   _nGetStrokeCap(long ptr);
    /* M3-ATOM kind=METHOD id=e976140f1b64e9e01a073939d9418e365d8627f5c38b2152ddd9669283b4222b contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetStrokeCap(long ptr, int value);
    /* M3-ATOM kind=METHOD id=662aaf03293a772c96d8f79ead682721f1c7f7768d462766017723174e93c53c contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int   _nGetStrokeJoin(long ptr);
    /* M3-ATOM kind=METHOD id=6df7b076a66f31aec169c2eca1b7ab8a7ab365615cc9a48b901e32df62456b21 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetStrokeJoin(long ptr, int value);
    /* M3-ATOM kind=METHOD id=71de3de17fb0e4c0390d081bfa510505d221b10a6aaf38af3568e9d10ebaa9c0 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetFillPath(long ptr, long path, float resScale);
    /* M3-ATOM kind=METHOD id=f426ea62cbb015d16db4c1caff1a0b2dbe76536857793373c711219de5e00122 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetFillPathCull(long ptr, long path, float left, float top, float right, float bottom, float resScale);
    /* M3-ATOM kind=METHOD id=5ec70e62cac617fdbb9b368179944cecd9a8a45b4a8a2862fdea6f022908efa5 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetShader(long ptr);
    /* M3-ATOM kind=METHOD id=1c41b5f1105ca332a55b41a0bdda2bac0f3abdb83900f2b6748a6e97af2e6c08 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetShader(long ptr, long shaderPtr);
    /* M3-ATOM kind=METHOD id=e83580c1763719689dfbe57dbc62a361bef600af3cacfd186b6b8a05bf9909f1 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetColorFilter(long ptr);
    /* M3-ATOM kind=METHOD id=c088cf025a5e71aab5de7974d0d1973349125f5129a248a90f3ee3d5c09e75e9 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetColorFilter(long ptr, long colorFilterPtr);
    /* M3-ATOM kind=METHOD id=86decb3d23487710340856bbcdfb918567142849d2e9a48471af750fe13886f5 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetBlender(long ptr);
    /* M3-ATOM kind=METHOD id=5e2f1ba215cf5e0a8cf557e68911f30c214c0dfd3c1afff52b07f2e99428e608 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetBlender(long ptr, long blenderPtr);
    /* M3-ATOM kind=METHOD id=2b866c655fb29916c8fd4f071a05039138c07d2e6c7840890013f829cd94159d contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native int   _nGetBlendMode(long ptr);
    /* M3-ATOM kind=METHOD id=c52169c92619aeaea9def4b971c3aed964251ed85aa63c168f3584e931b27762 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetBlendMode(long ptr, int mode);
    /* M3-ATOM kind=METHOD id=5a83b5ab42f53e40f760e3174bc081c0f7149d4565372f9d911859ac22cb90ee contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetPathEffect(long ptr);
    /* M3-ATOM kind=METHOD id=9baf267cee75f9ed56ef39466cc085ae39f8a4b378876a09ce827c7796f02868 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetPathEffect(long ptr, long pathEffectPtr);
    /* M3-ATOM kind=METHOD id=db889bb38e3f6fc0f7c30ae76871c3472cf0bc99b8755d3757e2f7de8145eaa2 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetMaskFilter(long ptr);
    /* M3-ATOM kind=METHOD id=69ee45d559027a5bae10e71971a4134b8329a44a0f5b7ef19bff68139fb41202 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetMaskFilter(long ptr, long filterPtr);
    /* M3-ATOM kind=METHOD id=c1319dd5b842116daea73b4b12ab92461decb779846f2be4cbc951c2110e0711 contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native long  _nGetImageFilter(long ptr);
    /* M3-ATOM kind=METHOD id=7953138633d2ca3016d28765edfb629812ee38277ad2a7644af936df121f53ac contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native void  _nSetImageFilter(long ptr, long filterPtr);
    /* M3-ATOM kind=METHOD id=774ebde545c98b94593389c6135233ee3df085d0f6db3302ebd7e15fdc6e178b contract=JNI_CONTRACT patterns=JNI_BOUNDARY */
    public static native boolean _nHasNothingToDraw(long ptr);
}
