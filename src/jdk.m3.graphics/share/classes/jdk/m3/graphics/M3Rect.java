package jdk.m3.graphics;


/* M3-ATOM kind=TYPE id=06187ba0095587591f2fe9f93317d7a76f0bc363f3961a149d2eda36d443ba51 contract=PUBLIC_API patterns=PLAIN_TYPE */
public class M3Rect {
    /* M3-LEAF kind=FIELD_DECLARATION id=9971ea9e308d3c90d3c084f636cf308e6d83975a705ceb7630486bbb71b6045f contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
    public final float _left;
    /* M3-LEAF kind=FIELD_DECLARATION id=8dfaab99ff971d9a4ccafb1dcfa436987bba4506fa95f8420c1fbb0debed1e5d contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
    public final float _top;
    /* M3-LEAF kind=FIELD_DECLARATION id=b6bb3f92f9de92e621742124f791f1482a3148f444a5688e91efe80b368c9eb1 contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
    public final float _right;
    /* M3-LEAF kind=FIELD_DECLARATION id=352cba3b9c9e332caa1be1ba3e5190cb9afb3efb3a44f4725f016cd6d7c5a0ca contract=INTERNAL_OR_PACKAGE patterns=FIELD_DECLARATION */
    public final float _bottom;

    /* M3-ATOM kind=METHOD id=8f764371ea48135c5b17467b684e10ed2130ddafe2b783d753dc17905bf7fa39 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect(float l, float t, float r, float b) {
        /* M3-LEAF kind=MUTATION id=7b82a8cf2081fc08aa8a2663e1316d46aab370a9580b46ad1499f99debf8cd86 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        /* M3-LEAF kind=FIELDACCESS id=de5a35ae5685ba48e11d180559e7e82b999696733985b9774cdf7d71a36eaa6c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
        this._left = l;
        /* M3-LEAF kind=MUTATION id=7866e8aad0686e42d539e238808c1ddc6d40de2f02d06e278dead2f02c573ec5 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        /* M3-LEAF kind=FIELDACCESS id=536d46fcbd4fe1ef09926edd9507e350d84feee177eb1cccc8bc20fd6e683321 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
        this._top = t;
        /* M3-LEAF kind=MUTATION id=f213d2c3966c2da874d3d90660634b99eac8442ff7e45518c9ec2cc68113cb4b contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        /* M3-LEAF kind=FIELDACCESS id=72b7068c59f93a8af75ebd9596240e6c92daa63e4c75e18d8cac9497ee22fa76 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
        this._right = r;
        /* M3-LEAF kind=MUTATION id=edf3fdea1096ba4f97723ec8d95347dc0d5bde3fe56d3dc4e3c4260e461f1d59 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        /* M3-LEAF kind=FIELDACCESS id=570f7f131fe62ff45d482b05d5dcf8717ce5b010da0e4d17830c9cb712d174ac contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
        this._bottom = b;
    }

    /* M3-ATOM kind=METHOD id=3a1bb1307fe6d0ed52a59e934b6ee69919766ec2aceafaae6cd7ba7c9d50ac57 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public float getWidth() {
        /* M3-LEAF kind=RETURN id=feaffb6f3a12075caae6d57fea2e94049f166f75d92830db2d988c185588ed72 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return _right - _left;
    }

    /* M3-ATOM kind=METHOD id=04c226fbae5b1e99fc228a56afe73d4ec7e96aa774066c566b403b4e056b2cb5 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public float getHeight() {
        /* M3-LEAF kind=RETURN id=418333c7a5612ab02aa70e9823290390008d1c1bd854f18910bf4d7aa1e62ba3 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return _bottom - _top;
    }

    /* M3-ATOM kind=METHOD id=a2b786c41f4f8fc38ba8855e8847c783e8d62962ac376f7de0f83b583b485a41 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect withWidth(float width) {
        /* M3-LEAF kind=RETURN id=7a2864691a67dfa670f105f4edca5c538440faba436ab6eaffc9cfedeced0994 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=25a680009693cef196eb56acbe7563243f55ee39e52f637b195fe05199ac4f1b contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(_left, _top, _left + width, _bottom);
    }

    /* M3-ATOM kind=METHOD id=a66677f26d47ee71d0fd51b011dd9278a36cdcf8913774fa06a7fd8deab030c3 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect withHeight(float height) {
        /* M3-LEAF kind=RETURN id=183dfefd8a2a06fb63be89bbb8b8d0ec7f9a298f6154e371f38f3042eb685d31 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=a38ea4a4ed268fe850f5e3f0fc90402167355a91e3394c9e15db1b1b4b2d1f7f contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(_left, _top, _right, _top + height);
    }

    /* M3-ATOM kind=METHOD id=6ae8343e7d1310be4d43825ffbbfd7eba95e897957269571e310b43e15d40d9d contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public static M3Rect makeLTRB(float l, float t, float r, float b) {
        /* M3-LEAF kind=BRANCH id=e150dec12f447fb00f7606062a862ed10d6596e370796ed9f8194ed417fa2df6 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (l > r) /* M3-LEAF kind=THROW id=cb29cd5013a6759006d6e27dbebfd5e36e1501ebd209e3d8539c86b5b98c6643 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
 throw /* M3-LEAF kind=CONSTRUCTION id=3d48dafc7b426889f5ffecb18637648e762a41056adbeb3367acf66fd8cfba0d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Rect::makeLTRB expected l <= r, got " + l + " > " + r);
        /* M3-LEAF kind=BRANCH id=4dddaef833d1619ca11145466978ddfc3696dea87a193ccdb29cbd3fc2d4beec contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (t > b) /* M3-LEAF kind=THROW id=7de5b16fc542fffb1c76e2d3a231e29f3cb125a270e8b8e8a1331d4b7f2a15a1 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
 throw /* M3-LEAF kind=CONSTRUCTION id=a14b434a2712a257a1603a92dd09824cfc7995cd5fa0ea13cc0fcd6ad0bcf5b5 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Rect::makeLTRB expected t <= b, got " + t + " > " + b);
        /* M3-LEAF kind=RETURN id=1b21bc8a9297239387f1a6ed2fc1d0534461f757ae85ccc1df1b63bdf9ddc050 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=e796b2adc83e272d19df309b933fb8e4af7ce27297e31b86c307f3af46fd6ab4 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(l, t, r, b);
    }

    /* M3-ATOM kind=METHOD id=d9db659096dfb4c78f139f464fa5d995e76dd6d2beca4ad5adcef216b7f02b8b contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public static M3Rect makeWH(float w, float h) {
        /* M3-LEAF kind=BRANCH id=0add39a61e1a61fa4d1aef5bdebb63abec000ae81e5c893f0ef5de8ac2c3ad9c contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (w < 0) /* M3-LEAF kind=THROW id=cfffc6c184982af157fd875bdff1fc39fdbaf77050c65fe357572ae9073302a3 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
 throw /* M3-LEAF kind=CONSTRUCTION id=1e9d81db2b21f622174b4a39439b1a54cf906df3cf8f9efc327fa761c8fdfc1d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Rect::makeWH expected w >= 0, got: " + w);
        /* M3-LEAF kind=BRANCH id=3b0abcbc2e66c84efc4b5155ef33b7d1d3c076474ba65cc461853ed8011d5807 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (h < 0) /* M3-LEAF kind=THROW id=48594317e4b211ff15c5f457412f0e0b5d8fa2792f7de17d51024b6f11914ea5 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
 throw /* M3-LEAF kind=CONSTRUCTION id=9b34a3bfa1123fff457d2fe418e7766b8bc40cfa0627424f1c622d7cf573e9ae contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Rect::makeWH expected h >= 0, got: " + h);
        /* M3-LEAF kind=RETURN id=ee8351c31c3f4b3de78046b84440a89e3dd81d7747c8877c9cd20ea3cdfe674e contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=fbe9d60c735e169b93bba0402ac4dbc2b13e6f5195a627c065dafc41ddb1368d contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(0, 0, w, h);
    }

    /* M3-ATOM kind=METHOD id=f2328e2abebeda1c7402ddbad81a2d4bfb889b7b60280e78e99104b56589b307 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public static M3Rect makeWH(M3Point size) {
        /* M3-LEAF kind=ASSERTION id=662ec6c93c8701a6a4dfa0356204507ffef5550b1a3f5cb816e24c3757190e10 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert size != null : "Rect::makeWH expected size != null";
        /* M3-LEAF kind=RETURN id=cab199f54e8cacb6e3733f0e59e6cb179ea00e6a89574bd231978272ba44bb7d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=81ab4844d2eb8121da089d052a9cd482132abf7ca683c9ef48e40e0e0dd21b06 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 makeWH(/* M3-LEAF kind=FIELDACCESS id=15fe409e90202b44d10077320c6516eaae415791c5e520215c6e3fbc980bfb66 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
size._x, /* M3-LEAF kind=FIELDACCESS id=cb9c4ad006e489f22d0e367cb882c512b5ae589f0f3e63333c49171a0309de1d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 size._y);
    }

    /* M3-ATOM kind=METHOD id=e67ee149bcc645d039ca26042fe0eb6e8672e20237a5f0ed15b982b3fcbf56d5 contract=PUBLIC_API patterns=BRANCHING,EXCEPTION_BOUNDARY */
    public static M3Rect makeXYWH(float l, float t, float w, float h) {
        /* M3-LEAF kind=BRANCH id=4ab62cbace3945e76b63c37f378f7b8f8e03243052f171c526dfbd0a88132954 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (w < 0) /* M3-LEAF kind=THROW id=aa92c52b212b4e6798b8237335cbe859624fa914631a93c1b6ae0d1d3f4aff0b contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
 throw /* M3-LEAF kind=CONSTRUCTION id=03c11b93e45594894f8ddcf7f3e146cf35d6ce5348526c66c7e54ada1c8cde6b contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Rect::makeXYWH expected w >= 0, got: " + w);
        /* M3-LEAF kind=BRANCH id=45fadc14a3b4ae0663ffbb1319e5acee3a984c144283f26a8f54e9511707dcdf contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (h < 0) /* M3-LEAF kind=THROW id=03ec4e84642e83f507f7e4489b7ebc1383c4ca4a1eb38eb5db3f010fcb7cda42 contract=INTERNAL_OR_PACKAGE patterns=EXCEPTION_BOUNDARY */
 throw /* M3-LEAF kind=CONSTRUCTION id=d2291db6a289c984961cd3daa3454a59518fc0ca5588d2cd91f9d9fc33b82914 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new IllegalArgumentException("Rect::makeXYWH expected h >= 0, got: " + h);
        /* M3-LEAF kind=RETURN id=a33b50893eadb9359456cec6a2cf7058b44d56c6e716fc7df1aac7b2afe3fd46 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=fa6bd2d3f358f87a43c6eaebd7ce8bfafe5bd3329bf933ccf146b584ad378f52 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(l, t, l + w, t + h);
    }

    /* M3-ATOM kind=METHOD id=33e993bf2f626a46f46a14580216ed42ea6efcea3f48f20f74f36509b38f6ef0 contract=PUBLIC_API patterns=BRANCHING */
    public M3Rect intersect(M3Rect other) {
        /* M3-LEAF kind=ASSERTION id=49b4c197fd320c415652bd938c23b36c10c60c522f8d47322f7b43837e91ba88 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert other != null : "Rect::intersect expected other != null";
        /* M3-LEAF kind=BRANCH id=1d72711ab0a051ded607ada40d7cd104761a7182e380f5e6693f17f8b6dffbde contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (_right <= /* M3-LEAF kind=FIELDACCESS id=1428f8071e64ecbdd0de6a753884d8a22b0ce4ae3379f24ec1a320ff90d368a9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._left || /* M3-LEAF kind=FIELDACCESS id=01a183e73ea34e9bee3df53d41f6acc2f0feeefe8ecc5480d5f2ea37943da25c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._right <= _left || _bottom <= /* M3-LEAF kind=FIELDACCESS id=59ec283d253b5514cb90177e77af2f8bae88bd6a9cc340ba4c641a3ea14883f6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._top || /* M3-LEAF kind=FIELDACCESS id=7ac358a4f37e2dd20cf0ca27ea470dcf1b21fe37ef9d5ec1f58cadb171192bba contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._bottom <= _top) /* M3-LEAF kind=RETURN id=593771dde11a424c9bf11dec1720a811f9f40d389c8846cf02e8fea6296c2ac1 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return null;
        /* M3-LEAF kind=RETURN id=9ad3e780fe4bd62e9f32b644bfd77fa3b01011b978875ba37627dc6f7145de1b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=547e1b67f4de42594b429d42b3b01f8b63393bb168dbf77c9cdb3a6044cc918b contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(/* M3-LEAF kind=INVOCATION id=59ace3f3c0a364e8d0bf103d539f3186595701135bff16f38030e10d287e390d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
Math.max(_left, /* M3-LEAF kind=FIELDACCESS id=292cf4bf992afed6999dd747676e0b6cc9028709c6f924d330b26803d8146140 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._left), /* M3-LEAF kind=INVOCATION id=abf9e0ac7e183a8cbdcf8222a4a1082397d8536906aeaa45cdc73116389ae432 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_top, /* M3-LEAF kind=FIELDACCESS id=50a470ba387fe0e684a28bcd97b054f610afe4e57e15b9cdaa53655a13d86b27 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._top), /* M3-LEAF kind=INVOCATION id=1e363d55758f79fe179627b968921c7ca3aa25e7aca20c1af47730b93690b8a1 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.min(_right, /* M3-LEAF kind=FIELDACCESS id=a3209314b748c335f2ab9497670cb06eda27dc51389e5de3bda044def61108ea contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._right), /* M3-LEAF kind=INVOCATION id=f357123475c6449e84b5602f5e7d36a7c6e7def742892e0e0a86d34e421b204d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.min(_bottom, /* M3-LEAF kind=FIELDACCESS id=8b652483b00f4df0caf9203c24d9cc4001379209dcad2e81b3725bc77d998db2 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 other._bottom));
    }

    /* M3-ATOM kind=METHOD id=3bc19b2453b9ed9667c6a2b83108786df5ec39dda26272682a48ed9c4e401745 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect scale(float scale) {
        /* M3-LEAF kind=RETURN id=94c96f1c0def4ba21f80a53c8d81c8f317deffd1b73a7415e1d7cae9356e455b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=42e19b428423b89d9f5e25af968d6d88cbf62c60d2d49bf06e294e24c6e39ff0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 scale(scale, scale);
    }

    /* M3-ATOM kind=METHOD id=55357398a4a0a4319d176175a6659829f0fbd29f96d31c1bf666cc92bd7c5abb contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect scale(float sx, float sy) {
        /* M3-LEAF kind=RETURN id=99a736d3a49efe39bb95496b40ef1c619bac7e9255281a17a6105ed67ef28903 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=20a2f11659dde2ed1ec1bb31e22a3a64491beec95d84a3946fc928e18dbcb25a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 sx == 1 && sy == 1 ? this : /* M3-LEAF kind=CONSTRUCTION id=fb73f3bded5a87e60ec4138252537ba7f441d768cbf0ce583d4ed5654661996a contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(_left * sx, _top * sy, _right * sx, _bottom * sy);
    }

    /* M3-ATOM kind=METHOD id=cf589034b7c3c886134daa315a46c3cf5ad16eb09a010e19f9e24da6f0930380 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect offset(float dx, float dy) {
        /* M3-LEAF kind=RETURN id=23216a029b06acc3caa3fe0113e1966eb65411a2d14ba4681595e82b7ce7794b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=3066ec818573ca62a365a0be455ac5d5de3f02336fa311accd2f1c524c8db3af contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 dx == 0 && dy == 0 ? this : /* M3-LEAF kind=CONSTRUCTION id=5fe34d077c5a94e832e0ca6b600dd8fc02908db8cd4b7ba0c8155ffe1e78acf8 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(_left + dx, _top + dy, _right + dx, _bottom + dy);
    }

    /* M3-ATOM kind=METHOD id=8d1c13d0795e5841fade340543b905d3aa53a3b3575018e06acbd808d1a2623f contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3Rect offset(M3Point vec) {
        /* M3-LEAF kind=ASSERTION id=d6f85aded4cd129e58c6b268d5e1ca19d3b02cbf6166e151502b1a9393289c87 contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert vec != null : "Rect::offset expected vec != null";
        /* M3-LEAF kind=RETURN id=3dde86c7027643839b03b546817709d2e07e6b651338895f96496e7a744274e3 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=6a327f535c072dd45904291d5183e012d1b9ce8cbc49dd3a8f5806130c99c7fc contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 offset(/* M3-LEAF kind=FIELDACCESS id=b9e374c3b8acde27fb901f19e1a0844ac712a4de53416d9f4c9fcff8125b03f5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
vec._x, /* M3-LEAF kind=FIELDACCESS id=5f2e1f6caa4d075bd2ee1b99b86e54810c9302924ea2651325f1873b8a0f0eed contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 vec._y);
    }

    /* M3-ATOM kind=METHOD id=58d37c8b5a0c4816b2a727047daa4c4e92db1c1278625432b1202cfadd212652 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3IRect toIRect() {
        /* M3-LEAF kind=RETURN id=beb00e27242882e4c7caf8a22d2a75d6e1e7500453e14a58a37a81ac40f27753 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=CONSTRUCTION id=f13ea50f3f7fbb576c4d06b94911e027df51afd73324cc754ca9682d36f99b12 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3IRect((int) _left, (int) _top, (int) _right, (int) _bottom);
    }

    /* M3-ATOM kind=METHOD id=1adc4eca43b5237948ecc507edd1d6ae946b6a35f806174fa7e12a4c579a7e6b contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3RRect withRadii(float radius) {
        /* M3-LEAF kind=RETURN id=72ca8e8b21a4589125d7468a3b019671f32e4f998fe2b8020d5ce0e71865d5eb contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=97cd2c9e5baafbe9db38f356bad17dd421a646fe6905dd534a9e21f2f81ad661 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3RRect.makeLTRB(_left, _top, _right, _bottom, radius);
    }

    /* M3-ATOM kind=METHOD id=5726afe78964bf98093defa395b9b62b623d2881ce9c20b805887763d32ba707 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3RRect withRadii(float xRad, float yRad) {
        /* M3-LEAF kind=RETURN id=1372e7806b40cbbdc646d6e12911ba069f7450b4d34c4702d7242da308501a23 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=36bd9e96e05c8f87baa8b53a4d44c2ca8e3e20714cf830141deb4ebd99b6d1e3 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3RRect.makeLTRB(_left, _top, _right, _bottom, xRad, yRad);
    }

    /* M3-ATOM kind=METHOD id=ed4bf617b360c52cb143e10f9fe40ba386612d6acb62db7f19f5a8ef54ff3eaa contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3RRect withRadii(float tlRad, float trRad, float brRad, float blRad) {
        /* M3-LEAF kind=RETURN id=757f9bfd72ff8bfb505d8451061ba5c14a715d957c81daab9d64fdcb32711ea6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=3dcccc05d10facb98a6dace676fc8e7527e6241ed2e1dc7352f5417889a17aba contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3RRect.makeLTRB(_left, _top, _right, _bottom, tlRad, trRad, brRad, blRad);
    }

    /* M3-ATOM kind=METHOD id=af9940181729f1a9af71734b1e46c9170412b7a439d3635f5f0a66ea56a5fe1d contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public M3RRect withRadii(float[] radii) {
        /* M3-LEAF kind=RETURN id=b0688378354eb3543b07bfc33c409aefc34c0fc6d0d94a7f76b19de66e5f4f13 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=INVOCATION id=8bd000aed96060bffc02a67d8de979d32dccff4a2aac93d60d61aae78ff2a95c contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3RRect.makeComplexLTRB(_left, _top, _right, _bottom, radii);
    }

    /* M3-ATOM kind=METHOD id=ed5c86433de6c3aeb799dee497df29c33588506ebe8ca2ba7f05d1fbe3d170e6 contract=PUBLIC_API patterns=BRANCHING */
    public M3Rect inflate(float spread) {
        /* M3-LEAF kind=BRANCH id=5610fa64c3ebe00f66a07e84b67da092792326d393d6db76bbb7930c26414a01 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (spread <= 0) /* M3-LEAF kind=RETURN id=db144efba78659ebc918ec65526573bb20e3fa9dbb41a46e8b06a9315dad367d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return /* M3-LEAF kind=INVOCATION id=9905a81f75af6c82d5e576bdd35ee8611bd1f8608ad10f397bd3464f389a9f66 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3Rect.makeLTRB(_left - spread, _top - spread, /* M3-LEAF kind=INVOCATION id=eae22a01c667b5a08dd55cffc1583de0eea730e940f578b1382b8d8e5a26a297 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_left - spread, _right + spread), /* M3-LEAF kind=INVOCATION id=dde815bd84cba5d885f94dcb75549374ff13df41057c665a10a8ace5d2e3e1a5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_top - spread, _bottom + spread));
         else /* M3-LEAF kind=RETURN id=acab21ca69524b854b3fa2762aec0a1f7f39bf72755c8e65414c3fd80a295977 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return /* M3-LEAF kind=INVOCATION id=843d65d8bfb30c3874a077c14739bea47b758f1ed3ee5e351ded5ca064cd50c5 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 M3RRect.makeLTRB(_left - spread, _top - spread, /* M3-LEAF kind=INVOCATION id=531c439972425e9ef96848cbb06d84c65b6da3f95deaad6744eeac5b30b70c52 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_left - spread, _right + spread), /* M3-LEAF kind=INVOCATION id=8f4cdfded7d09fc4432d7386f7dfa98458ba14d15cf1187dfeb5579ec410a8dd contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 Math.max(_top - spread, _bottom + spread), spread);
    }

    /* M3-ATOM kind=METHOD id=da1cfc713b7d378c59397087d7d575d61b4da527c761fe86b8ec581a88ceea79 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean isEmpty() {
        /* M3-LEAF kind=RETURN id=06130ac23f6808ff2a2d8363747c825254ba1bdf92bd5eac20f8858bfad6dced contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return _right == _left || _top == _bottom;
    }

    /* M3-ATOM kind=METHOD id=4c11239c52653dafcfe109fa85ee23296d87ebfea9bfde349525c3b9b9f426cb contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean contains(float x, float y) {
        /* M3-LEAF kind=RETURN id=67a7b259afb3b332c7a23fac3238125f32b4c80c5e83ef1a74cd3da0746f3aea contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return _left <= x && x <= _right && _top <= y && y <= _bottom;
    }

    /* M3-ATOM kind=METHOD id=9d9eb6048b662a76ac99ba8b065ba15dc2a927f8f3bb062512714a855125450e contract=PUBLIC_API patterns=STRAIGHT_LINE */
    public boolean contains(M3Point vec) {
        /* M3-LEAF kind=ASSERTION id=34c092008ca3b746677ecddc637ac5ce6e3413102ff3b540261a6575b35c6e8d contract=INTERNAL_OR_PACKAGE patterns=ASSERTION */
        assert vec != null : "Rect::contains expected vec != null";
        /* M3-LEAF kind=RETURN id=0d18c51b2f5510f3236749714935e5f3f9565cc631f205d41a9aa3c0e725ea2b contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return _left <= /* M3-LEAF kind=FIELDACCESS id=d16e9c4421088c5f5b3a739af8abe4d71d6a0eaf7c5fc2ee5c6530a78b909c97 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 vec._x && /* M3-LEAF kind=FIELDACCESS id=86d327af236917852876b371e9075f447fd16a093837eeddd53779801179ead5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 vec._x <= _right && _top <= /* M3-LEAF kind=FIELDACCESS id=a35686741807f8131a24883a373080c8f5f17b243e912e31c1013593f2617f05 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 vec._y && /* M3-LEAF kind=FIELDACCESS id=26a3ba996cd6022b2ac169aede79f66cc6b0fffb673ffd835f792c692a74ce4d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 vec._y <= _bottom;
    }

    /* M3-ATOM kind=METHOD id=cc7ca222be8f0ddac19ddba4f5f9fde9a67df10425d6d5f6a2844610a2f110f1 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public float getLeft() {
        /* M3-LEAF kind=RETURN id=c7d701ac84bcfcfba9ade484663bb9211add8d21b30b11909c8f8299a967337f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=FIELDACCESS id=729d8f14ee43aa7c08b90b701ceaea57c0335139af8c29fcfef2536eaadd4761 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._left;
    }

    /* M3-ATOM kind=METHOD id=2778db5ca0e6cbe34f86296a5afa00fa16a71ce111bb9f8b9a897796e5a6a57e contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public float getTop() {
        /* M3-LEAF kind=RETURN id=ca9ce4684ef8a31a6e9901a9e855647ef6e78dd41c6cfec845ad02fb7bbc1bed contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=FIELDACCESS id=eb15671be45a84fd501535e2db6e23ca1c1d23f17fe807904b1856f36b8b6b7a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._top;
    }

    /* M3-ATOM kind=METHOD id=3a019cb2773828a4e6d3054c1c655c0163bc6c213c4877bd7a4436444b390606 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public float getRight() {
        /* M3-LEAF kind=RETURN id=1363acda451fe36bb0e857f4c63790e0d2b6d0fc353cdcdc954d41557ff5137c contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=FIELDACCESS id=6800bdd8e6b7429edbaea3a419e21f67e08ad01f817477e2b51c96df50fb8212 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._right;
    }

    /* M3-ATOM kind=METHOD id=0333287b1f887a50ba7056fe45558177b639d9995a6c82c2fea81afec5e2d64e contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public float getBottom() {
        /* M3-LEAF kind=RETURN id=96f6ac683fe52df2f9e773b2617115b5fb6eddf4d11f2ad0022a066e33e9ed61 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=FIELDACCESS id=9cfd93de84eb102c9319a72ed2142594cb6f0f492c784352b980cd5968d0bc6e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._bottom;
    }

    /* M3-ATOM kind=METHOD id=6778d3c799a74273fc9654534236304663bb7953c1e667030cb75079fff0428b contract=PUBLIC_API patterns=BRANCHING,TYPE_TEST */
    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        /* M3-LEAF kind=BRANCH id=a1c2740dc984cd23ce98a2656c534d37b217da69cb3abb2f82291b1e20674a35 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (o == this) /* M3-LEAF kind=RETURN id=ac6c14013f981edd3049149165e4f9c5a32833720ffcdf1838074931943ee5f7 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return true;
        /* M3-LEAF kind=BRANCH id=803a94f22167ef6940dc408fa0bcf52a668702c550cea8f5ed0203e8b29a8ec1 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=UNARY id=e1c30edd3a87e1235cd1a336be58415d046b31b623331f6e12a414072cd521ff contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
!(o instanceof M3Rect)) /* M3-LEAF kind=RETURN id=1621ceb901f4930e0bf754757d8170c718f8aaea7e4590a1927a03bfc4e2c440 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=LOCAL_DECLARATION id=18f685aba886f62399753790671570b5385cb80f294efaf7dcd2d010c45560df contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        final M3Rect other = (M3Rect) o;
        /* M3-LEAF kind=BRANCH id=4fc9f8da1ae123d623e0c255a141296b8af8fa8323b96a3dbf5d802150730e0e contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=UNARY id=3ff64776baeb328fd3cb2f6fe57697484ffea1d26f969722ee94ece1f5435aa6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
!/* M3-LEAF kind=INVOCATION id=bc7b60b2be3d3e1f724522e20a0e55711b4773aab6af7acde7886552c2298f47 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
other.canEqual((java.lang.Object) this)) /* M3-LEAF kind=RETURN id=48cbf7c6478251187779cf2925be8f90cbe3a0fbe20a2c7925fccab6c8617bcb contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=BRANCH id=f719a0458627dbf9148d916bf5b26b0f286baef9e24714ed6e0516ee2a2adfdd contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=INVOCATION id=f0d0a642536ae23d83e59567a5d10cd4f2c59a313db2b347e33c047bf86c1a70 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=FIELDACCESS id=a9d9773115657f9577053ff7d62c40954f33d1160c08738e57f32f28d4d5eaf5 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
/* M3-LEAF kind=FIELDACCESS id=9431f66dd42e9abce045fe19b2d5d22d446a5351ccfdc8a69ee87c592f1de127 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
java.lang.Float.compare(/* M3-LEAF kind=INVOCATION id=7bf0ae7850bde2db95210d4c876f15391e09ec135002d00cd2e91dc25b684668 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getLeft(/* M3-LEAF kind=EMPTY id=7fd809e424977f78d02b08d53e89a701520d0ffb82c7d3a940e10a5b5f77fdc7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=cd62b67411317cfd2e43067a4a034d5915694586265856b392620ef2fd385086 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 other.getLeft(/* M3-LEAF kind=EMPTY id=5fd27e5b21b271d7e52cb72e617b9390392f3aac35a204d2fabd1388ea4e4d0b contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)) != 0) /* M3-LEAF kind=RETURN id=7c84e1576bf878caa824d2a03c300a0c04c716c2647c29c678fe87f85ad6f93c contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=BRANCH id=01e9c095d37dd21b12fe0145a59fa3ca539b82f3c32d8777ae6a4dd9090f5846 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=INVOCATION id=cb4d743d8f4230ef13de32b73eac75fa3bcb717f94577ec494e48eee786affe8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=FIELDACCESS id=823e7d23680dccc814d1a20fd2f05b4fa98e97726dc51d2d8c6c7a1cef46f557 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
/* M3-LEAF kind=FIELDACCESS id=5e6c401d01343ff8d19c181eb84ea89c4ee9ea873a50d37e2a8be7a505149b01 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
java.lang.Float.compare(/* M3-LEAF kind=INVOCATION id=88fb81ee0837834bd9cefa0d9f4f3a1ddcb1753a62da13c3442b8610ebc57cc8 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getTop(/* M3-LEAF kind=EMPTY id=cf8e03680b34929d7f03c96e00e91095b10abefa0db92fdc75312ca0c83ef851 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=db6118eaa89191d126a63a8fc1f92c76660cfb0ecbf3dce82cfef63a5e6a084a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 other.getTop(/* M3-LEAF kind=EMPTY id=b162a3b067510cbd3ab87269b411abb39a23c95044db9036db2814834f406d1c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)) != 0) /* M3-LEAF kind=RETURN id=287199a46d616ebedddd93a19e7460864d33fb91e41a801e87171272b5573fe6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=BRANCH id=8bd1d6b604202b09c3b284701852579dd4c880319ba7cef8b37592f9cf6f72d0 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=INVOCATION id=1100709e846091b24b94aea263ec07acf10b2f1fd1a5a91f5ccf26c4fce9eb7f contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=FIELDACCESS id=4cd6e36624a69e60e6e7ea36d0a3db61275efce049d5cb453bad87e6007fc116 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
/* M3-LEAF kind=FIELDACCESS id=04fa50b39bfaace63cc530da9977a80a6999a0e742290840ea16a8b7ba40d2d4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
java.lang.Float.compare(/* M3-LEAF kind=INVOCATION id=01bd404cc1ee6068f22cd71f14463db3618adb5a24d41b66ac0ed42d818acc83 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getRight(/* M3-LEAF kind=EMPTY id=90707cb5cb86875f7dce0336a28bd552fbc5cad352de79fa0e27f7c52be61149 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=bb6395f6caf1daece34f1c817735e3b935af45c0ca6544e9572b053c33489cbf contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 other.getRight(/* M3-LEAF kind=EMPTY id=f4d7387da618c1ddf90dd5ced9805763a32fb9e0b7be070266f9a30ce4e0ceed contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)) != 0) /* M3-LEAF kind=RETURN id=0889d92e5207c821b88a72d4ead01841fafac2c9c90243095ca4f345b4de7924 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=BRANCH id=a011518eb41151b8babcd799028a2d92c537a6622a3c7579513e84b0a4630ec8 contract=INTERNAL_OR_PACKAGE patterns=BRANCHING */
        if (/* M3-LEAF kind=INVOCATION id=3329961180db6b02d02e378230ed065207bc23312ae4a2e62b06917ef4d3bc86 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
/* M3-LEAF kind=FIELDACCESS id=566417662c7377c25e7cbffbc69b2be3bf81cd3bc95f873fb9dc6bbe6b315be9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
/* M3-LEAF kind=FIELDACCESS id=3dc0aa55b6ae06442e102bd3beb2acb945ad6905d1e5d3264d98ddad35031263 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
java.lang.Float.compare(/* M3-LEAF kind=INVOCATION id=a9d070f4307202e85caba8ab3feb6f69b9f197a319d7c4bfb530ef75b678dc96 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getBottom(/* M3-LEAF kind=EMPTY id=a16d5b952719ede3b6e851182c92835752a7b9414dfaa31dd7704d99b819d699 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
), /* M3-LEAF kind=INVOCATION id=b3e2080736e67d2da28691def5d32e97c40c53ffa58603d9b904ddb014852fef contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 other.getBottom(/* M3-LEAF kind=EMPTY id=b2cd08a13c5a837e0540fff8696f6ebe36e28f2cb12e3703bf34e94e9f54aad4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
)) != 0) /* M3-LEAF kind=RETURN id=48737df9782ea670a256da2ddfaf43675c1fd5a87110313a61fff50c63d99d4f contract=INTERNAL_OR_PACKAGE patterns=RETURN */
 return false;
        /* M3-LEAF kind=RETURN id=67d7f2adcc4ad36014aef47f7f9723e4028d549a44567f83192635f1972e6fea contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return true;
    }

    /* M3-ATOM kind=METHOD id=9a44f7a8271b7b23bb7e5f5f4a73aec49be3f0f2b112193c5355c7dae2dd4af2 contract=PROTECTED_API patterns=TYPE_TEST */
    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        /* M3-LEAF kind=RETURN id=a0d42b32b1ded3ebd907bd0bbf73d057df273b45510d47ee6c01a76968ec90e4 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return other instanceof M3Rect;
    }

    /* M3-ATOM kind=METHOD id=d15f05687acaaf2acad4c620abe079b1a2e9e0577585d443d9785ba5350553ba contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        /* M3-LEAF kind=LOCAL_DECLARATION id=dc4ffbac7f0a5018fc6f7f2bf75c52dfa3c27eb6ac57f74427a0a7a41fdd0a4b contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        final int PRIME = 59;
        /* M3-LEAF kind=LOCAL_DECLARATION id=5a4b0058fad787f615e4bb2b494965f0180d5f829be66f148c14b4ba21414967 contract=INTERNAL_OR_PACKAGE patterns=LOCAL_DECLARATION */
        int result = 1;
        /* M3-LEAF kind=MUTATION id=91dcae8dfd8afc2b0c9745e68c4cbeb4ca11a162645e20d040b4337f41f863de contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        result = result * PRIME + /* M3-LEAF kind=INVOCATION id=ad7ee428a30893b6d25bf9e266596dfc4c47ae87ad3c7175f178d348251438fe contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 /* M3-LEAF kind=FIELDACCESS id=c93fa116ea038470d9c65d5beb9759d2dd53cda474820c49701afd5ef872eeb4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=58746ff1c04797b3aa7148ceec72ef0d6d7d77d052128a2e30d3b75d0ef53019 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 java.lang.Float.floatToIntBits(/* M3-LEAF kind=INVOCATION id=6df670493c8b37a370a49f1e4812f22149b79481af8b4a06e36ec9c39c1fc1d4 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getLeft(/* M3-LEAF kind=EMPTY id=b52deb98a335036999aacaa2fc2b074b32d7aaff718ed8d9ef0b106d07673c8a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=MUTATION id=6e869cbc710f40c37db8b47e534146ec8165ac48fefdcdf5e0f53fc7640dfa4b contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        result = result * PRIME + /* M3-LEAF kind=INVOCATION id=894c2e1e4d49fbea4956073a60ea6442321fd748b56f4dad9761f42184f6af35 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 /* M3-LEAF kind=FIELDACCESS id=7bd101ec798144265184d31c72816bb3fab7ee9bbd5ec2853f3c590c8fc9117a contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=8f4a97e7cae831789feae0c05b33f418f4c6c3618e4834b7f1ccf0751f34ec91 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 java.lang.Float.floatToIntBits(/* M3-LEAF kind=INVOCATION id=2682b4644ad1342ec7fe38ac6810e2ade5a9b2d852780b38e0ea63f79092acb7 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getTop(/* M3-LEAF kind=EMPTY id=85a2df896b73cd70411536c4b53f1dd474e505da79718243957a782a45c1d178 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=MUTATION id=2e4e5b0014998dc276e012c6232c11e9e30c64fc992445df94d05e481292aa30 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        result = result * PRIME + /* M3-LEAF kind=INVOCATION id=09b174d9ccc6af8991c0c2fd28cab1750c93fb158b19d6c4ce143b9da626e75a contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 /* M3-LEAF kind=FIELDACCESS id=bae3e4da3ea5db8fcd3362a3d9496be56e00dabec6275f53797cbb49e654566c contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=721b639adf6fd959ed876469175d172266bfb5e54b0b5796e52f81bb284d89b6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 java.lang.Float.floatToIntBits(/* M3-LEAF kind=INVOCATION id=bbd75a91a9359d9261eae7dcea08bc448c8e442fa4edfc4cc2e0326b7e7bb1f0 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getRight(/* M3-LEAF kind=EMPTY id=9c1adc292c0b6294be6b8d7731bd3814bfe0e6ad93ed6c7891e7643c699cdfff contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=MUTATION id=3a15503184cad299fd5b30fcf103975f2f272d3dcf7852d824e560224a500e30 contract=INTERNAL_OR_PACKAGE patterns=MUTATION */
        result = result * PRIME + /* M3-LEAF kind=INVOCATION id=8f76e7a8eb33c01ba714f1c56e8cdac7da9eb2e99749bbc7a577d4cfddc10703 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 /* M3-LEAF kind=FIELDACCESS id=6d5c51c733afc80ec28f84d48c8610a2408ddd1b5e271278816e5b66aa54151d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=91d4b19bfbfa21826504ceae3f0d89925ca7656628af9e35647ed7b347d69180 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 java.lang.Float.floatToIntBits(/* M3-LEAF kind=INVOCATION id=8e7f42bb119b68a0992422b3d20a437adaee8667019c286f9531e6e78386ecba contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
this.getBottom(/* M3-LEAF kind=EMPTY id=f5521c353d086fbaadcacb30fe91877daf9716565acaa101334200d544e1ddbd contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
));
        /* M3-LEAF kind=RETURN id=aa9a1640c7a7a241c42ae2b0e44c900ce079f6b6c2ce8334de76ffef9792e8b6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return result;
    }

    /* M3-ATOM kind=METHOD id=083425119582423687077e61905e9d2221a76b3ee3c1582e03ffb09d96f0faa4 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        /* M3-LEAF kind=RETURN id=e35ec01a28ac3f9bdaeb7dea9f6655695386ec46ecb4cf93e24e0fcbef50c3a6 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return "Rect(_left=" + /* M3-LEAF kind=INVOCATION id=6df377dfdf4907a04a08ec597d79c3d8625128877441ace715ed3e05c9bdfe83 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 this.getLeft(/* M3-LEAF kind=EMPTY id=0536ccb5b463a46f18fda86d0551f6c33e3c2ccc594fd8b38cc88db37e57ebc6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
) + ", _top=" + /* M3-LEAF kind=INVOCATION id=262d02eee1fe90e1847480d4a682145a3955252ea4eb19ca45fd53f82497e77e contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 this.getTop(/* M3-LEAF kind=EMPTY id=29ff01b35965565b94be50dff2f1cff84384cc26cdc53d9e113eff5b66516f85 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
) + ", _right=" + /* M3-LEAF kind=INVOCATION id=aee6a2cf68ec0968193b4e27aadf89202105769aeeea38da60b84c716bb75d9d contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 this.getRight(/* M3-LEAF kind=EMPTY id=f3d4a01746359a6dc6d703e9a5cb06ce23c27fa8a9d710beb4a50778ea6256fc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
) + ", _bottom=" + /* M3-LEAF kind=INVOCATION id=d84254a6d1d90fe7b1cf99c00be703f8401555cbec290dc533721ec7d9ecb5e6 contract=INTERNAL_OR_PACKAGE patterns=INVOCATION */
 this.getBottom(/* M3-LEAF kind=EMPTY id=823efeef8b0e47f015c04a7e73efae5f4ba7600483052863258e32f71e2e537e contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
) + ")";
    }

    /**
     * @return {@code this}.
     */
    /* M3-ATOM kind=METHOD id=82e218b5e655c071b2357d905c23ddaabdb29b67fa17fc8c7b8c6b9391299c94 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public M3Rect withLeft(final float _left) {
        /* M3-LEAF kind=RETURN id=9ac0dc8e2bdd3f79d2422729194467d35f932ceb924a1317656098ec9497b62d contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=18839386213b3faa3822a72347024a729292307f02d401b6b112a681acf34604 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=c877800279baccf8bbff7962cf09e8a97e6b006dd4454138e39383349c49f5e0 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._left == _left ? this : /* M3-LEAF kind=CONSTRUCTION id=30f6c9abe77137cc10315de54211e0a412e3cdc1271975b8bc6fd68a7cbe0157 contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(_left, /* M3-LEAF kind=FIELDACCESS id=c502d892b6c08b2b16e7cde07c9f7904894da9ed0b1c72af738a4ef312a4033f contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._top, /* M3-LEAF kind=FIELDACCESS id=ec4928d515443d50f42e4603e15779a2bb3ce056d826831bbf9c0369f7fd0c44 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._right, /* M3-LEAF kind=FIELDACCESS id=e539ab71dee1817bbc207b1bba2c6726ebf9241abcea8830f4838d15cb3f265d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._bottom);
    }

    /**
     * @return {@code this}.
     */
    /* M3-ATOM kind=METHOD id=5e0ae1ba662b8acd4caa84cf1afc8d6b1f93e7d691130309b7dea5fd935072b6 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public M3Rect withTop(final float _top) {
        /* M3-LEAF kind=RETURN id=8c8f9a403580feb172a50be6d0788348e7c29bc58a7af47df2721e9d4efc8c23 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=47b4d57f5b6029d36cd094996018849041930d9e67e9f02a383068938c5705d7 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=be8e88625e407397b09119679f3a33ab383e24532b4266fa55b3e770891d06c3 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._top == _top ? this : /* M3-LEAF kind=CONSTRUCTION id=94ad41da143d9507955c668ce4e5676f48c22aaf8aebe60eb688906d7a04fc4e contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(/* M3-LEAF kind=FIELDACCESS id=4a4598f67945e665c0b14dbfa2756a6738a55b813d0d0adf9a544917ccb0f3e4 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
this._left, _top, /* M3-LEAF kind=FIELDACCESS id=ab9be8edb341818e4c0b7df43010f6713a51d6bf3f90a6d9e00678c8cfd37bbe contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._right, /* M3-LEAF kind=FIELDACCESS id=35e68c73c34586a262b41b0fd7878017fc6013c60893fee8c65afe65262e0caf contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._bottom);
    }

    /**
     * @return {@code this}.
     */
    /* M3-ATOM kind=METHOD id=270404a65c7d894c4e0f30e260b1edc4eddf8db65505ed46dc48dc178b3a4c16 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public M3Rect withRight(final float _right) {
        /* M3-LEAF kind=RETURN id=507fb64fe04a5c03b84df1334a83617a7752e5dd23a45163fb741939eef99939 contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=4d94690bce1fb7bb1d704aecd0268133594c20be28e9a9e01488bc8edf8aa28d contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=76b59fee3e9b1786457cb6356963d71bf618109de8bca4c7c07570af63031ec9 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._right == _right ? this : /* M3-LEAF kind=CONSTRUCTION id=d5a6198411d5c130f976ee0872caad167c034ebbcb2525cb421a96cf80e1da4a contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(/* M3-LEAF kind=FIELDACCESS id=61e89679680065cd763fdcef85f1a70b59047a45c208d5cb13daa2d3cb3476cb contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
this._left, /* M3-LEAF kind=FIELDACCESS id=407bf618fbb8bb9880c732fd5732e272b86bc4f3138ce5c0d62d654c1d3abdd1 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._top, _right, /* M3-LEAF kind=FIELDACCESS id=457ea68476675f47ba0053ffa9db7d12748faa21ff006ae69a2275313482f534 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._bottom);
    }

    /**
     * @return {@code this}.
     */
    /* M3-ATOM kind=METHOD id=6bc05c22f59123f61809cda67ce87d22c82e75ed4e62ffd68d8d6791381e2c59 contract=PUBLIC_API patterns=STRAIGHT_LINE */
    @java.lang.SuppressWarnings("all")
    public M3Rect withBottom(final float _bottom) {
        /* M3-LEAF kind=RETURN id=7e32327935a51eb74ee4c6f1b3c422cf114d91a91736b15b59b7aee84bd3a9bf contract=INTERNAL_OR_PACKAGE patterns=RETURN */
        return /* M3-LEAF kind=TERNARY id=c1c969b192f906e4416f22aa89133858d28b4058305fb53a26f35f87b533c7bc contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 /* M3-LEAF kind=FIELDACCESS id=e6ded66cf5b6e6adfa8a992b1ef298f7dad9bd24c8c235e760e394026254e677 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._bottom == _bottom ? this : /* M3-LEAF kind=CONSTRUCTION id=aa0027e875f6d54bd864796ac1f22dda4d1fea0271d3e9a6d1b3b8a9e2943bcb contract=INTERNAL_OR_PACKAGE patterns=CONSTRUCTION */
 new M3Rect(/* M3-LEAF kind=FIELDACCESS id=864903aa2cd732d10415a3b02ed2ac7875d974d4cd3fcb3d13b24cf38f16b004 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
this._left, /* M3-LEAF kind=FIELDACCESS id=294dcd67c81227c9255b53c8fce2b818ca9532fd57d511066accba44080807d6 contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._top, /* M3-LEAF kind=FIELDACCESS id=ef673fafbc05f5e90c6136fa272b5689e672e2be2529ab378f02c783247b05da contract=INTERNAL_OR_PACKAGE patterns=STATEMENT */
 this._right, _bottom);
    }
}
