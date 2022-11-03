package com.catchyou.controller;

import com.catchyou.pojo.dto.ApplyCodeReq;
import com.catchyou.pojo.vo.ApplyCodeRes;
import com.catchyou.pojo.vo.CommonResult;
import com.catchyou.service.AuthService;
import com.catchyou.service.VerifyCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/verifyCode")
public class VerifyCodeController {
    @Autowired
    private VerifyCodeService verifyCodeServiceImpl;
    @Autowired
    private AuthService authServiceImpl;

    @PostMapping("/applyCode")
    public CommonResult<ApplyCodeRes> applyCode(@Valid @RequestBody ApplyCodeReq req) {
        int code = 1;
        String message = "请求成功";
        String verifyCode = null;
        int expireTime = 180;
        int decisionType = 0;
        if (req.getType().equals(1) && !authServiceImpl.checkPhoneExist(req.getPhoneNumber())) {
            message = "手机号不存在";
            return new CommonResult<>(code, message);
        } else if (req.getType().equals(2) && authServiceImpl.checkPhoneExist(req.getPhoneNumber())) {
            message = "手机号已被注册";
            return new CommonResult<>(code, message);
        } else if (verifyCodeServiceImpl.withinOneMinute(req.getPhoneNumber())) {
            message = "验证码请求过于频繁";
            decisionType = 2;
        } else if (verifyCodeServiceImpl.isFrequentRequest(req.getPhoneNumber())) {
            message = "需要进行滑块验证";
            decisionType = 1;
            verifyCodeServiceImpl.recountRequest(req.getPhoneNumber());
        } else {
            code = 0;
            verifyCode = verifyCodeServiceImpl.generateVerifyCode(req.getPhoneNumber());
        }

        System.out.println(req.getPhoneNumber() + "的验证码为" + verifyCode);

        ApplyCodeRes res = new ApplyCodeRes()
                .setVerifyCode(verifyCode)
                .setDecisionType(decisionType)
                .setExpireTime(expireTime);
        return new CommonResult<>(code, message, res);
    }
}
