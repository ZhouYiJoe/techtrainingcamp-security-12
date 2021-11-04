package com.catchyou.service;

public interface LjhVerifyCodeService {
    String generateVerifyCode(String phoneNumber);

    boolean checkVerifyCode(String phoneNumber, String code);

    boolean withinOneMinute(String phoneNumber);

    boolean isFrequentRequest(String phoneNumber);

    void recountRequest(String phoneNumber);
}
