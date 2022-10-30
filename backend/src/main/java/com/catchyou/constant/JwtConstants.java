package com.catchyou.constant;

import java.nio.charset.StandardCharsets;

public class JwtConstants {
    public static final byte[] JWT_KEY;

    static {
        String jwtKey = "XIr*$2wVT4BkMT6M";
        JWT_KEY = jwtKey.getBytes(StandardCharsets.UTF_8);
    }

    public static final long TIMEOUT = 1000 * 60 * 60 * 24 * 3;
}
