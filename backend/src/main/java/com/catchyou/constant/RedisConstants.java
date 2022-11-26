package com.catchyou.constant;

public class RedisConstants {
    // login_state:用户ID
    public static final String LOGIN_STATE_KEY = "login_state:%s";
    // login_limit:IP
    public static final String LOGIN_LIMIT_KEY = "login_limit:%s";
    // private_key:经过Base64编码的公钥
    public static final String PRIVATE_KEY_KEY = "private_key:%s";
    // captcha_fail_count:IP
    public static final String CAPTCHA_FAIL_COUNT_KEY = "captcha_fail_count:%s";
    // captcha_block_list
    public static final String CAPTCHA_BLOCK_LIST_KEY = "captcha_block_list";
    // ip_black_list
    public static final String IP_BLACK_LIST_KEY = "ip_black_list";
}
