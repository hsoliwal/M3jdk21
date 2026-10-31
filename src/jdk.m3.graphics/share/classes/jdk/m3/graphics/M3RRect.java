package jdk.m3.graphics;

import java.util.*;

/* M3-ATOM kind=TYPE id=30d9d804b6a1ebea6efe494b4bda27aa7f0a53030fcb220166fa1b4263ada555 contract=PUBLIC_API patterns=INHERITANCE */
public class M3RRect extends M3Rect {
    /* M3-LEAF kind=FIELD_DECLARATION id=5047ed2174c65ae432b2a252fc6e969118cb272e2c947e14a7b5424c3d4500f6 contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
    public final float[] _radii;

    /* M3-ATOM kind=METHOD id=474df13edca833955fb8daf8e3918f79739df087541c25107edce969994534ff contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3RRect(float l, float t, float r, float b, float[] radii) {
        /* M3-LEAF kind=INVOCATION id=0be57ebbc997d07557346936bb290a4cd0fd47e9866c4b5d8f242d7b703c1a19 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
        super(l, t, r, b);
        /* M3-LEAF kind=MUTATION id=06d032dc891923cb3b5c7d1786b6fd5cd0ecd11ef1d2e267f4fd5ea53c7403df contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        /* M3-LEAF kind=FIELDACCESS id=aab9fea63d7a47c786f0faa3142518e34fecd259a5f4e4a2a2bf50922e995e12 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
        this._radii = radii;
    }

    /* M3-ATOM kind=METHOD id=ae47e27ca326f64cb3a3942a5b1c9b1fe74c74f7be8913fc82d4f12a7ba039c3 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeLTRB(float l, float t, float r, float b, float radius) {
        /* M3-LEAF kind=RETURN id=5352be63b211130622de20f6e26c45ecfdd235a373e0d70d0b7f24a55f3543c8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=891648008f062230f72b64b84dbf56016eeb5861186f38d8bed1f7c651395bce contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, new float[/* M3-LEAF kind=EMPTY id=994b15803aa86958a498b19908d446db304c07e6f01b4e4484a63389f60521b0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {radius});
    }

    /* M3-ATOM kind=METHOD id=a077e4b336e3c00ec9bed62699ed9928ba6a079ef006f6c71b2a362361208e44 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeLTRB(float l, float t, float r, float b, float xRad, float yRad) {
        /* M3-LEAF kind=RETURN id=c1e32b8c0ddc85df37e13d0a84197ce10fd84148cd008e4a77c19c0718dd8ad5 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=cfad8fa20e93ced986edb0bc40b2e56e9a61f71fd4b2fbead44befa42782e755 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, new float[/* M3-LEAF kind=EMPTY id=f26819386129640017cc12d8242c0e094e769ee21ea4d452c7684f84f6c62493 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {xRad, yRad});
    }

    /* M3-ATOM kind=METHOD id=2f6755d657354670e1fb9490ef85c2aa77105bfde5daa5d84eab1c4fb58b76d5 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeLTRB(float l, float t, float r, float b, float tlRad, float trRad, float brRad, float blRad) {
        /* M3-LEAF kind=RETURN id=30fb268692f09cd2056345134d9056203717d2945b77b9b1f7983405711c00a1 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=05ff99dbe3acda0de7bb74d9d8403878284d921417cc0c1a918351e3575534dd contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, new float[/* M3-LEAF kind=EMPTY id=764bdfd0f98d8f512b41ae6031dcf5c7fe4cc18b5fc66b01c7cefc05223c6832 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {tlRad, trRad, brRad, blRad});
    }

    /* M3-ATOM kind=METHOD id=d8a246d8bb110cea9aca02a9ecfe3ccf7f71224ddc596f110f69f23735cf2c7b contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeNinePatchLTRB(float l, float t, float r, float b, float lRad, float tRad, float rRad, float bRad) {
        /* M3-LEAF kind=RETURN id=c2b215f1db724333e76d61c7e3d13f4035a1e3ac2830cae32a13ea15845a3326 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=df52f0ed90990946fda943df6fe79286275f3a41887fd3160c664d4b9a205cf7 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, new float[/* M3-LEAF kind=EMPTY id=8d00cabda632965585acb61b6f96649d4404e35f6091c6bbd771b63c89cc15e2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {lRad, tRad, rRad, tRad, rRad, bRad, lRad, bRad});
    }

    /* M3-ATOM kind=METHOD id=46b35d4bc5a3ce53300eb682c8acf3279c6c4675f9dcb476ca337a23b515fca1 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeComplexLTRB(float l, float t, float r, float b, float[] radii) {
        /* M3-LEAF kind=RETURN id=632e9d5b55b47212e3a5494853412bee1136f95f8e4d13b786e13e44d051fa68 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=ca2f74a983411f6efd28da573dfb60723a54a8a3ffda4e5da39462541b27755d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, radii);
    }

    /* M3-ATOM kind=METHOD id=61961175b844773323af7d5d24ede700f0bfaa768042f9aa72f799cfb44079dc contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeOvalLTRB(float l, float t, float r, float b) {
        /* M3-LEAF kind=RETURN id=a00f26008f17309743338b635d72e8b9eeb807f53d54e14449f454db392c1db7 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=53fc66678f52b86b94018afdc7c1237b4ed494be293f35354564fe497d336c51 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, new float[/* M3-LEAF kind=EMPTY id=3194b5d23294344787e7e78b2e4f975b123c2e3ad0b1fd4bd98cabb62032b039 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {/* M3-LEAF kind=INVOCATION id=f593095f828fb5906b896b404a464cc9e7b441955586ec5a851f017a54d2bec8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
Math.abs(r - l) / 2.0F, /* M3-LEAF kind=INVOCATION id=db49a0c8a2e1e56523fa3bea2ad6c536dec64607d8d88b85664e78df6fa4a527 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.abs(b - t) / 2.0F});
    }

    /* M3-ATOM kind=METHOD id=414ec172f9137bc37dc96ca512a4a0d0b1594c85d46fac41e740e67049d90b62 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makePillLTRB(float l, float t, float r, float b) {
        /* M3-LEAF kind=RETURN id=9e5de47f00b02150c71dc5836a1e98b68988410d041d6924417f9ac507dbd054 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=86b5a928a36d6d7836a4c6e623e2cb0779afe77103c2cdc00ac23944eb78d60c contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, r, b, new float[/* M3-LEAF kind=EMPTY id=ebc3b2cb1e93a175b263b93e2c3b9434f631bf79ae89c84b4098e3d561038ebd contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {/* M3-LEAF kind=INVOCATION id=76aa35bf57e6f3be7e4d0974ec62980a50c2bdb3b93aae8e80895e6bfaf649e2 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
Math.min(/* M3-LEAF kind=INVOCATION id=19e131e203ebb098558e6fcbe06dec7bed07ef428bd80b095b9a92b1580a7b7e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
Math.abs(r - l), /* M3-LEAF kind=INVOCATION id=b465f2d6427293b1ca71cbcafdc7d1faf3a07b8530d1da7a72080a44f8319bd5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.abs(t - b)) / 2.0F});
    }

    /* M3-ATOM kind=METHOD id=8f11f634946cc44f3aac6ebb70a0178f26231ee10f6e73ba8bbd1f71fe465e1a contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeXYWH(float l, float t, float w, float h, float radius) {
        /* M3-LEAF kind=RETURN id=a7f43aface7e22517a7f122cb9cd91ae6721509f7ff7927d91df30545295186a contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=c8223e09a486e49285147ae37dbede0553fe283b676a08ca3196ea1c16b07ad1 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, new float[/* M3-LEAF kind=EMPTY id=eb5cfc962493ca72e539e15e323d892a9da9cdc62c1bd17bc06ab7c2353bbb85 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {radius});
    }

    /* M3-ATOM kind=METHOD id=eda13a6a9171d2ff243a530044d5734699ab22f9ae515767a25576dbdca7774b contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeXYWH(float l, float t, float w, float h, float xRad, float yRad) {
        /* M3-LEAF kind=RETURN id=d9d47d56cfff4cf1054735b43e2f47cc080316c523663a360c7e7b99ddc86a13 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=9533ddd9a4d275a2b32853042a768f8c4abb44b8d9da6358d96de2169cb88780 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, new float[/* M3-LEAF kind=EMPTY id=0a797466ecaeb30a4aaba3a0ef58594c94f398acb70b7f41454641abc75b7b1f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {xRad, yRad});
    }

    /* M3-ATOM kind=METHOD id=770da6d9a8fdeaae732755187fa2c4f83f35e7a4bd06e11bfe05812d6622392f contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeXYWH(float l, float t, float w, float h, float tlRad, float trRad, float brRad, float blRad) {
        /* M3-LEAF kind=RETURN id=262f131a29c501a3239f350ae64dda04e54f2ef6054b1af8d08a2299e4d14f78 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=17c9cd05cbe3a513e63b4b39d324323848842423e4dec85c63ea02cc85cecdc0 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, new float[/* M3-LEAF kind=EMPTY id=02c67926f03a4d77b04599995c9f97cdfe8c1e83862c40f351b073735ad5156d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {tlRad, trRad, brRad, blRad});
    }

    /* M3-ATOM kind=METHOD id=98b3cbded99264dd84dc52e45414cd2dd21b41ad34462740f2165c50884d7bd0 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeNinePatchXYWH(float l, float t, float w, float h, float lRad, float tRad, float rRad, float bRad) {
        /* M3-LEAF kind=RETURN id=92e4c89e6df95f217200014d82d518276c0b093e40c8ba8f54673af2cb6c14c8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=50dbb73696ab383966e1545e0c337f454352fa7b4bfbc5bf42df6f96fe44fc58 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, new float[/* M3-LEAF kind=EMPTY id=96f7ab18b96628ed35ce21916ea4315d41c85a4af5e1e633c47dbce35567efce contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {lRad, tRad, rRad, tRad, rRad, bRad, lRad, bRad});
    }

    /* M3-ATOM kind=METHOD id=fe8d921729adfaa641e78fbf5902ef7979c6876ae4b58fe9d9dc10d1f480c036 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeComplexXYWH(float l, float t, float w, float h, float[] radii) {
        /* M3-LEAF kind=RETURN id=ff120d85cb8530e98f27b656a93be88eca037366c6647d289d57b2446fdda6ee contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=107864e5143982b2aef3e455eb7700ba5335744b2bb10a5a8547ea9db6e298b7 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, radii);
    }

    /* M3-ATOM kind=METHOD id=2024cd3c314f0f96f191f58355751f75dc876472146ed543d6ac53f8b74414ca contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makeOvalXYWH(float l, float t, float w, float h) {
        /* M3-LEAF kind=RETURN id=b29c948374ecd4cf2de3c13b4d41f5e6e1c95c880023bb60cd01b3e0bb901ac4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=5fb469381bcc41794535bc8e9686ec3d5951bcd8a5b55674928de970b8fddbec contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, new float[/* M3-LEAF kind=EMPTY id=b0ccd7aa6ab24bde23f2c1f022f4e52ffb077a3f73f977e6ec332bb35381651e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {w / 2.0F, h / 2.0F});
    }

    /* M3-ATOM kind=METHOD id=5dba83bfaa25cd1f7418ab031074f89fdf28c86f41cefd249dc30c43b2717e08 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3RRect makePillXYWH(float l, float t, float w, float h) {
        /* M3-LEAF kind=RETURN id=f0f01359d768ec76a3dfa779aebad6472e8b7cec7be954cfe51ddac76cf37f26 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=be9fcb7ed53ed3593e2cef721801c21ed2c27f86cd5f3f9ee5e39072602ab9d3 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(l, t, l + w, t + h, new float[/* M3-LEAF kind=EMPTY id=c156aee3d0c71402fec51a1cae489f83c474817f925066bf141c6e14cee670b8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {/* M3-LEAF kind=INVOCATION id=2c6812f96594d4f00b1038b88ab8c83b8cafd2b1de4d7a1d125f06a33324eb26 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
Math.min(w, h) / 2.0F});
    }

    /* M3-ATOM kind=METHOD id=f69be834f51742a9c9e3867b48a02c18946f25c00ad7bb81015a9bd89cf64f19 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @Override
    public M3RRect scale(float scale) {
        /* M3-LEAF kind=RETURN id=e7c1c03ea8ebc15c4e70cb0b533d30a59786f75e5867faf812a6dbd9c221f2d3 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=22cc497eae107d34d9a8d69034793ca2f37f61181b43ec1f5aaa6727a9b89243 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 scale(scale, scale);
    }

    /* M3-ATOM kind=METHOD id=4d9e30fd12bcbafeec7de5ca4f47f92ffab846c8eac82fd28cdee6ecbc7ee6e2 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    @Override
    public M3RRect scale(float sx, float sy) {
        /* M3-LEAF kind=BRANCH id=f3da67971a391ba1a110650381a2b580a86ef8e80513795b7dcac830a8d06722 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (sx == 1 && sy == 1) /* M3-LEAF kind=RETURN id=d08bd50f952983952ac8cc2c3a3cab1c7c30f6a8e386f37dc7f5f871c1542b9d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return this;
        /* M3-LEAF kind=BRANCH id=8ac4aff72c35c30d62255eebb7371a687ced7b41d731c86a01b7ce804641b232 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (sx == sy) {
            /* M3-LEAF kind=SWITCH id=cd7295205063f9cd26b00e3df0f93390bcbf1a517bed0db8edd92d85b82c074e contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            switch (/* M3-LEAF kind=FIELDACCESS id=486dea10b46b15d4441a7d42c90db6589da6de90ebce5cd6640acf51797470dc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
_radii.length) {
            /* M3-LEAF kind=CASE id=fa9ff377e3c8584eb1718c21773b578f6bcc3a4f3f646bd56c317d0a030131d7 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 1:
                /* M3-LEAF kind=RETURN id=e5486de4242086a59f0e1bc88bedf708d3046d84afe1be26dc2427cddf9f6139 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=f419558d82343416e685eed3f7224cc8aa186f3f968a2bc4daf154467ab6762d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[/* M3-LEAF kind=EMPTY id=53420cd959deba6314d6b001be4eb4ea83ac208f26e372341dbbbb69dc0c84c6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx});
            /* M3-LEAF kind=CASE id=c4236a5360a01de93e1a8bd532d4a5e0c36f73b7dabffa28f0dd6d842a67a50f contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 2:
                /* M3-LEAF kind=RETURN id=acbb460a518bf89dbec36a4b952525f195ed98ba6ca62c8c32ec10cc6b678e44 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=720be0f4f61ed7ae4bdb45a92e9379b6ca2c1e5e5795c98abc1349212b6d35ac contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[/* M3-LEAF kind=EMPTY id=9faf17fbeb43871e5711ae0fa5ff642a68443e284ecd74084d62f362ec5e8bec contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[1] * sx});
            /* M3-LEAF kind=CASE id=c01514ffeaa819c26f2a071db91154b282f00fab61c4a338ffc08a3a65fda102 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 4:
                /* M3-LEAF kind=RETURN id=efa9987b9c4358cc62c4940e0230a49152cac0b130fe0de7b83972ca15940ac6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=c5c884517a104ce329640dae3dac047d2d70cdb94ea1b0c6bce8b21829f61478 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[/* M3-LEAF kind=EMPTY id=dadfe135dc0a1cf6a1232a34c1c10c94efac2232c10134829cac9cb3f4c56569 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[1] * sx, _radii[2] * sx, _radii[3] * sx});
            /* M3-LEAF kind=CASE id=e9f9a10b984a5ca400030affcd3b580ddc1d2672aef21950ef3788c1b71ebe1c contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 8:
                /* M3-LEAF kind=RETURN id=3204392dd032c432ebe406ac445a928737d83ea5f097bf25d8928231531d0a67 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=073ab7bd4634ab3a0dbaae445dc80d03d2b1c6edbffe48d1edc08ad09523edbb contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sx, _right * sx, _bottom * sx, new float[/* M3-LEAF kind=EMPTY id=943401cfa6057869d49b1e417c33a0b7f87eb3cd05ebe2dbd8be13907466b8a4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[1] * sx, _radii[2] * sx, _radii[3] * sx, _radii[4] * sx, _radii[5] * sx, _radii[6] * sx, _radii[7] * sx});
            }
        } else {
            /* M3-LEAF kind=SWITCH id=2dff7ca43555c51f0795e46ad415b62db6437c51469d30cdfbde7e08eb75f085 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            switch (/* M3-LEAF kind=FIELDACCESS id=4c773b8fc471e4896f1f1178b889b50cb92a95fd5223b2e06fb520106869d140 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
_radii.length) {
            /* M3-LEAF kind=CASE id=e75a654b92b7c0e0309b0cefaa6059fb37548d717cc92c2e6952520a0a78a732 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 1:
                /* M3-LEAF kind=RETURN id=3f5268b189321838e9c88fa6a5ed86097c961bed8715c9b0fc42790c8826aa36 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=31a975bf31249045d03d42078774b0956260652c587333a385e4afed7aefa190 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[/* M3-LEAF kind=EMPTY id=428d1e7b8c7796a714d718a79fed826279daa2e94985edfd88d17b962b3f55f8 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[0] * sy});
            /* M3-LEAF kind=CASE id=3ad9e53ea919137de1f3fd80f6292eec8ac9963c39129f2b7290f86ee3b7f5ca contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 2:
                /* M3-LEAF kind=RETURN id=64c1f2c20a9f5d64b8f27a22cce2e8fdf87d508f029d743b03179837192f8aa6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=3673bc090bbce7d4a2b951b572a9ae84ac4b0448b112520f4645188a3587510d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[/* M3-LEAF kind=EMPTY id=7991a9d60fcba3c7707c93cbb9f0008aff1a30e2bb0139136caa5af3f4ed17da contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[1] * sy});
            /* M3-LEAF kind=CASE id=aeeed22c522ab72326ce76ec82d94c7b65dcf67c7c679147b6aa16442a0db3b8 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 4:
                /* M3-LEAF kind=RETURN id=39661e547e1821e227dae3734e79aced8aee049eb4cfe23303b8103fdf76f507 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=a35a29fec9f69a890df7e958f4a77eb3ef0f686f4ac6511048e58e1d1cee794d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[/* M3-LEAF kind=EMPTY id=089fac7c8fe185e823ea7e4b6f19164641d0602208c4aaf35e21b02d93e00818 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[0] * sy, _radii[1] * sx, _radii[1] * sy, _radii[2] * sx, _radii[2] * sy, _radii[3] * sx, _radii[3] * sy});
            /* M3-LEAF kind=CASE id=277a9c11676c41f46eb1b29619086988ea9d36d7888f4fd6db9cecf5754fe72b contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            case 8:
                /* M3-LEAF kind=RETURN id=2cd48a7edcb2dad9bc88702a6ac97744693d0b100b2268a8333f7055e57b4ab1 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
                return /* M3-LEAF kind=CONSTRUCTION id=35b44007b88c9fe33f1efa38ff209ca65220dbcddeb184a0976933a5acc6b603 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left * sx, _top * sy, _right * sx, _bottom * sy, new float[/* M3-LEAF kind=EMPTY id=6efe1d244323a947a191f344da475b1f8c9f4ce0ee31ac5c6dda5594b8d7dcb4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
] {_radii[0] * sx, _radii[1] * sy, _radii[2] * sx, _radii[3] * sy, _radii[4] * sx, _radii[5] * sy, _radii[6] * sx, _radii[7] * sy});
            }
        }
        /* M3-LEAF kind=THROW id=4130ad4781b1c45407c976ba325ef8ec1b8e99aa7d4289c99ef724c616959e2c contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
        throw /* M3-LEAF kind=CONSTRUCTION id=47e6a5c18568ea068d48d765b795ec849861bae5c7bdfe66f490932565752c6a contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new RuntimeException("Unreachable, _radii=" + /* M3-LEAF kind=INVOCATION id=0d97ebef156eea1fb35e87b191a9adb27a521927542f420ac77d6558f778b15f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Arrays.toString(_radii));
    }

    /* M3-ATOM kind=METHOD id=5a61ca77504e8cf99913d36e3323145bd71636dfe31b4b5c3250d62afd64d946 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @Override
    public M3RRect offset(float dx, float dy) {
        /* M3-LEAF kind=RETURN id=ef735b8b28cd2ab1b6a8e4cc3b7ae593f9ea1ff6cfb4e1c8468e2f4413601a89 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=4ba85ef329351eaf28095759d1866a0cf6f8224e16d57b671e72c677332cecd6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 dx == 0 && dy == 0 ? this : /* M3-LEAF kind=CONSTRUCTION id=c11937398d40d69e86558a45e1e735267fe81a2f8eb3b925a94361bf29771411 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left + dx, _top + dy, _right + dx, _bottom + dy, _radii);
    }

    /* M3-ATOM kind=METHOD id=7948a0a0067e20078b57d0b22d1a86eb3793ac45fa31277a7c53aa900a118275 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @Override
    public M3RRect offset(M3Point vec) {
        /* M3-LEAF kind=ASSERTION id=90f2d71cc3a9a4902ffec9d47ecef03a3265f888a012bebdc12d5b65231d4b8e contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert vec != null : "Rect::offset expected vec != null";
        /* M3-LEAF kind=RETURN id=1005254ee0ce595cfffe3ff447d5fb5e6b650abaa47e82dd0303a71d54e946bb contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=2d93e86a8589280122f6f4ad61056f0cc244af8a1835e3fa8d5fd142b7cfc31a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 offset(/* M3-LEAF kind=FIELDACCESS id=df89b8bdd902a7a1308c56f3c30341975873672e45f1c70a8d56d5b9fa88f7c2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
vec._x, /* M3-LEAF kind=FIELDACCESS id=2e588dd099273e80e35439b3800667cdf97efda22fee12c9d43a5e5e2d68b2a5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 vec._y);
    }

    /* M3-ATOM kind=METHOD id=d2731249c6eeec197cbd145b5cf2427458063428fdbe97c70a077cab4e35c056 contract=PUBLIC_API patterns=BRANCHING,ITERATION */
    @Override
    public M3Rect inflate(float spread) {
        /* M3-LEAF kind=LOCAL_DECLARATION id=8612bceb94a1b87587246ec6bbe9d2427466792842dd5a9b7561b954bd76b47b contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        boolean becomesRect = true;
        /* M3-LEAF kind=ITERATION id=f6d3a90b478ec1c28cde7daefc9f8b57bafa298f8477cbd683ffca5d52543ed2 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
        for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=834b653ee03108fd4eee7b6df4c5ed70bc099985dca80fa183a1fbacec706546 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _radii.length; ++i) {
            /* M3-LEAF kind=BRANCH id=37f24dd1e2e1ce482e57e29fe52cc3496fe076a9b7de4d508e0a6cea94f5230c contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
            if (_radii[i] + spread >= 0) {
                /* M3-LEAF kind=MUTATION id=61ca1879b00009df7a9deb2a406008473c9d055bc0a3d6f12a7cb8e1e95e7d54 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
                becomesRect = false;
                /* M3-LEAF kind=BREAK id=88e0010a6fd4d0dfd6194cdd9a697c56d816b525b994023683b68afa5cd99174 contract=INTERNAL_OR_PACKAGE patterns=CONTROL_TRANSFER */
                break;
            }
        }
        /* M3-LEAF kind=BRANCH id=59cf71f38dba4a8c2c44300de7e49c20e9c84bbe21b0042dc6dee38e0b44afc5 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (becomesRect) /* M3-LEAF kind=RETURN id=46f064b53e15779888e26c137b4d1707e100b114de0ada1abce5b21db2764fae contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return /* M3-LEAF kind=INVOCATION id=37373d7467f85fbde161a064720b0dab09a8ed9e3bf0a67b2b08cda70853f4a6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Rect.makeLTRB(_left - spread, _top - spread, /* M3-LEAF kind=INVOCATION id=1c85f1a90892b3d3378291570b5c7d87d420d57f58bba8585f494e6db19fe1f4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_left - spread, _right + spread), /* M3-LEAF kind=INVOCATION id=8cb58e7a53e39533baf6cbb6ef25e7f68d919644c88786aedea121c77d624dd9 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_top - spread, _bottom + spread));
         else {
            /* M3-LEAF kind=LOCAL_DECLARATION id=224981f0a5150a41efb7d90f327a7ac73b91347354b857ed0b13d4a6b7b14760 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
            float[] radii = /* M3-LEAF kind=INVOCATION id=653c71ad7d4489e44a6cf087305e394fe650994b2e1b78018129a507a1ac8007 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Arrays.copyOf(_radii, /* M3-LEAF kind=FIELDACCESS id=3f9f6f989a53bcfea35ab5df3e31d58a0e964a0ce70b40d69f822e3e38e63055 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 _radii.length);
            /* M3-LEAF kind=ITERATION id=b7ef95162a6946a7e17424eda7cdf83bd37fa605a621165b2691353c9703d236 contract=INTERNAL_OR_PACKAGE patterns=ITERATION */
            for (int i = 0; i < /* M3-LEAF kind=FIELDACCESS id=008ccee468c24146f7389a1fc21032637cec999f0dbd16886bea6bb543f4828b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 radii.length; ++i) /* M3-LEAF kind=MUTATION id=520d1475027b689321eb8458b42c11437d57d35099451df415ee19527e94401d contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
 radii[i] = /* M3-LEAF kind=INVOCATION id=db97ae714407d1196016ca327275c5349bf10ba3498ae51020f3a58828b1f267 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(0.0F, radii[i] + spread);
            /* M3-LEAF kind=RETURN id=85284ee0890ff2fde9683ae4257d825a6bca1c44d6ab8e32d30eb8fd0d4b03a8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
            return /* M3-LEAF kind=CONSTRUCTION id=b4554132f50886aaedb51ce36e2694b796bcdc7b00e5226b66ab5ad7f0115abc contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3RRect(_left - spread, _top - spread, /* M3-LEAF kind=INVOCATION id=c1fcc37ec1d680902142deeb8cb10994d8046a9b49024b929d5e3b400f18d28d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_left - spread, _right + spread), /* M3-LEAF kind=INVOCATION id=500fcd1a204113d60696437ff1e3f07cfc41f30512085758f71a9fb3b5a635ab contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_top - spread, _bottom + spread), radii);
        }
    }

    /* M3-ATOM kind=METHOD id=c6c7784560b28a04b6f99d8b7a16d729ffd65642abd6a97a01d58431c7ad2256 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @Override
    public String toString() {
        /* M3-LEAF kind=RETURN id=a716a7bb2392eccc3fc8216cdbf97dfec05426a8516e172c8584cb98afb10379 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return "RRect(_left=" + _left + ", _top=" + _top + ", _right=" + _right + ", _bottom=" + _bottom + ", _radii=" + /* M3-LEAF kind=INVOCATION id=b14ad572eae403347777ed2de93496dd392520cd2f1b014d13580c7790553e89 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Arrays.toString(_radii) + ")";
    }

    /* M3-ATOM kind=METHOD id=a197eca1efe1cfc422a1ffd4c5a4a4f929ff8e98b1214a03e1d1d5a7876b12c0 contract=PUBLIC_API patterns=BRANCHING,TYPE_TEST */
    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        /* M3-LEAF kind=BRANCH id=5c338f731de4760b56f7a10ea057ebbd895a2e23bcb80ff0d0964171c8dd7597 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (o == this) /* M3-LEAF kind=RETURN id=d71eea81a82d0780b9158ac5e0e1fade95f24df9aface40081b0f732aef622e9 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return true;
        /* M3-LEAF kind=BRANCH id=e5b98960d10d9058bdedd5c20c3338564aef143f5bc4e416559085bd86072db4 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=UNARY id=a5bc7b46a3859078ec8e6767627120e413dd3e0953bf8fe8c8b51a78e21a129c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
!(o instanceof M3RRect)) /* M3-LEAF kind=RETURN id=5d9dcdd9d7f1fe7bb78ef02bb033a1d86abba626b8a402fd05603e89a22252e0 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=LOCAL_DECLARATION id=9daa124686b8d6dc09ca30376d3c01eb10e7532fa7dd2487e989b0418dc2ff90 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        final M3RRect other = (M3RRect) o;
        /* M3-LEAF kind=BRANCH id=fad248af3410cb027b293d1b7ec3d01586a3b506763c3e78ee0ab653d43bb8f1 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=UNARY id=a284c4370df6a7c76b4f553ff4b6b4e85a065d0cd0a03101ada8af548b332740 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
!/* M3-LEAF kind=INVOCATION id=31481790311e2e7e2a91f2665c3731704dca2fd315fe9bcb061de0527c5d493b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
other.canEqual((java.lang.Object) this)) /* M3-LEAF kind=RETURN id=44dc66a17734d1d31cf576ffa1cf374c23e392dbfa75d6367d4617a172cb1b00 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=BRANCH id=276d6f38f15d59a6d84dbfbaa3b4b326cd47fb03c3231548e83e2d10898d9869 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=UNARY id=458ca3ae34917d67a06be73dd6c6b9f8ff780dda06b2799450d36a8e78dffdcf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
!/* M3-LEAF kind=INVOCATION id=803551ce099d50a2a3f8dfabcc666f705cd0c6e7900adbbd5ca96ceba4a56d0b contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
super.equals(o)) /* M3-LEAF kind=RETURN id=730bc513711ab4adf94eb4dec2a14942446c4d8e78e9c0404399587458d9cbca contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=BRANCH id=80b16c11498533bce77b9f20700299edfebea49935004378f216b81d70f18c7e contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=UNARY id=d3cf4407df5955f871e2c2644c0623547f2c530b95d62054b438ab54c2db59c2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
!/* M3-LEAF kind=INVOCATION id=da7e85d927e12c1033c0c2016f36386a25ebfad5eb2cea46d086120adc546b41 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=FIELDACCESS id=54ca47d5a37d3267ae4c7ee796235d6994cc40ebb4beae3d91472ab66aa2bb47 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
/* M3-LEAF kind=FIELDACCESS id=e0c7c855ad7547551d64eb1e63757b8573e0c78742748a5ed4d3e1bb5bf5eeae contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
java.util.Arrays.equals(/* M3-LEAF kind=FIELDACCESS id=23fdbdbe4f233436b468d01c29adf4633738493f8305f2901a0990706f0a37cf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
this._radii, /* M3-LEAF kind=FIELDACCESS id=63b430ae4719ac3ec10c6c9bfbd6eb595be12076cef5eda68e9b05342f73ebcf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._radii)) /* M3-LEAF kind=RETURN id=2bc0cd1c1d852db3565c137c7d3c26857ea4215696cb782d06da03da214f7059 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=RETURN id=01f2f828ab14e9c1b8a26b7a77b113c87456e8dba21db5db7472b9c293bcd138 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return true;
    }

    /* M3-ATOM kind=METHOD id=63e461a894ff9b221d52726bb33dc9742105e785b82af4b124779e4462dc325f contract=PROTECTED_API patterns=TYPE_TEST */
    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        /* M3-LEAF kind=RETURN id=a1a0aa74aca620d47a99e4435e879023214e07943745b6bc147bb19f839751a8 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return other instanceof M3RRect;
    }

    /* M3-ATOM kind=METHOD id=e2f03d586f25789a3606937d63ee757ea88ec58636517e0237e80315127854e0 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        /* M3-LEAF kind=LOCAL_DECLARATION id=85532cf4b262f593390e5696187576313961e69889a47d7207c4364842b6918f contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        final int PRIME = 59;
        /* M3-LEAF kind=LOCAL_DECLARATION id=fa1d41483abe1673c7b466772de1a663d0315ad38745f358d449d8a8e456b3b2 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        int result = /* M3-LEAF kind=INVOCATION id=d87c80f22b6ca0642007d42605d7abe9aa24e0157c04ad2a6eb18908e36c920d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 super.hashCode(/* M3-LEAF kind=EMPTY id=7912ab7bc12bd30493aaa695d2d730f969cfa2101bf28b63e9d7a9af407d1b97 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
);
        /* M3-LEAF kind=MUTATION id=07b15dca7158cf3bd86c20a76f88d74a4d39ef6dabb2c8c36b603d709b6ea7a8 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        result = result * PRIME + /* M3-LEAF kind=INVOCATION id=bd83c670d3067c22ff3163b4f36f1b836507e48e8fdb523c38073fd2cbd6b3f8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 /* M3-LEAF kind=FIELDACCESS id=06986a6b9609c38d7c98be94a564760b80d5b3b9e9784101ec2a68bbeee224e2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=7b97f8c1497e6ed03b9493e8670825fc7051dfbade1a965b03eaff4352fe6cf9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 java.util.Arrays.hashCode(/* M3-LEAF kind=FIELDACCESS id=4ae43ea18a5478c6931d9223757efb4c0faccb67e0f75192258737b80e72621f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
this._radii);
        /* M3-LEAF kind=RETURN id=2a22b169aa591c89455be906c21c163ab23a42ba22234d03c5e191223f9193f7 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return result;
    }
}
