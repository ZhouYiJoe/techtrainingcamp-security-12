package com.catchyou.controller;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.catchyou.constant.JwtConstants;
import com.catchyou.constant.RedisConstants;
import com.catchyou.constant.UserType;
import com.catchyou.pojo.Log;
import com.catchyou.pojo.User;
import com.catchyou.pojo.dto.LoginWithPhoneReq;
import com.catchyou.pojo.dto.LoginWithUsernameReq;
import com.catchyou.pojo.dto.LogoutReq;
import com.catchyou.pojo.dto.RegisterReq;
import com.catchyou.pojo.vo.CommonResult;
import com.catchyou.pojo.vo.LoginWithPhoneRes;
import com.catchyou.pojo.vo.LoginWithUsernameRes;
import com.catchyou.pojo.vo.RegisterRes;
import com.catchyou.service.AuthService;
import com.catchyou.service.VerifyCodeService;
import com.catchyou.util.SpringSecurityUtil;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authServiceImpl;

    @Autowired
    private VerifyCodeService verifyCodeServiceImpl;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String getDecryptedPassword(String publicKeyBase64, String encodedPassword) {
        String redisKey = String.format(RedisConstants.PRIVATE_KEY_KEY, publicKeyBase64);
        String privateKeyBase64 = redisTemplate.opsForValue().get(redisKey);
        Assert.notNull(privateKeyBase64, "公钥不正确或公钥对应的私钥已失效");
        RSA rsa = new RSA(privateKeyBase64, null);
        return rsa.decryptStr(encodedPassword, KeyType.PrivateKey);
    }

    @PostMapping("/register")
    public CommonResult<RegisterRes> register(
            @Valid @RequestBody RegisterReq req,
            @ApiParam(hidden = true) @RequestAttribute("ip") String ip,
            @ApiParam(hidden = true) @RequestAttribute("device_id") String deviceId) {
        try {
            //判断手机号是否重复（发验证码的时候其实也做过了）
            if (authServiceImpl.checkPhoneExist(req.getPhoneNumber())) {
                return new CommonResult<>(1, "手机号重复了，注册失败");
            }
            //判断手机验证码是否正确
            if (!verifyCodeServiceImpl.checkVerifyCode(req.getPhoneNumber(), req.getVerifyCode())) {
                return new CommonResult<>(1, "验证码不正确，注册失败");
            }
            //判断用户名是否重复
            if (authServiceImpl.checkUsernameExist(req.getUsername())) {
                return new CommonResult<>(1, "用户名重复了，注册失败");
            }
            //判断是否为垃圾注册
            if (authServiceImpl.checkRubbishRegister(deviceId)) {
                return new CommonResult<>(1, "该设备已经注册过多账号，请注销部分非常用账号后再试");
            }

            //验证码、用户名都没问题，就可以注册了
            //准备一个user进行注册
            User user = new User()
                    .setUsername(req.getUsername())
                    .setPassword(getDecryptedPassword(req.getPublicKeyBase64(), req.getPassword()))
                    .setPhoneNumber(req.getPhoneNumber())
                    .setRegisterTime(new Date())
                    .setRegisterIp(ip)
                    .setRegisterDeviceId(deviceId)
                    .setIsActive(1)
                    .setType(UserType.NORMAL);
            //进行注册
            String token = authServiceImpl.registerAfterCheck(user);
            if (token == null) return new CommonResult<>(1, "未知错误，注册失败");
            RegisterRes res = new RegisterRes()
                    .setSessionId(token)
                    .setExpireTime(JwtConstants.TIMEOUT);
            return new CommonResult<>(0, "注册成功", res);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult<>(1, e.getMessage());
        }
    }

    @PostMapping("/loginWithUsername")
    public CommonResult<LoginWithUsernameRes> loginWithUsername(
            @Valid @RequestBody LoginWithUsernameReq req,
            @ApiParam(hidden = true) @RequestAttribute("ip") String ip,
            @ApiParam(hidden = true) @RequestAttribute("device_id") String deviceId) {
        String loginLimitKey = String.format(RedisConstants.LOGIN_LIMIT_KEY, ip);
        String loginLimit = redisTemplate.opsForValue().get(loginLimitKey);
        if (loginLimit != null) {
            LoginWithUsernameRes res = new LoginWithUsernameRes().setDecisionType(4);
            return new CommonResult<>(1, "因为之前密码输入次数太多，被禁止登录", res);
        }
        try {
            String decryptedPassword = getDecryptedPassword(req.getPublicKeyBase64(), req.getPassword());
            Integer match = authServiceImpl.checkUsernamePasswordMatch(req.getUsername(), decryptedPassword, ip);
            if (match == 1) {
                return new CommonResult<>(1, "该用户名不存在");
            }
            if (match == 2) {
                return new CommonResult<>(1, "密码错误");
            }
            if (match == 3) {
                redisTemplate.opsForValue().set(loginLimitKey, "1", 1, TimeUnit.MINUTES);
                LoginWithUsernameRes res = new LoginWithUsernameRes().setBanTime(1 * 60 * 1000).setDecisionType(2);
                return new CommonResult<>(1, "已经5次密码错误，1分钟内禁止尝试", res);
            }
            if (match == 4) {
                redisTemplate.opsForValue().set(loginLimitKey, "1", 5, TimeUnit.MINUTES);
                LoginWithUsernameRes res = new LoginWithUsernameRes().setBanTime(5 * 60 * 1000).setDecisionType(2);
                return new CommonResult<>(1, "已经10次密码错误，5分钟内禁止尝试", res);
            }
            if (match == 5) {
                redisTemplate.opsForValue().set(loginLimitKey, "1");
                LoginWithUsernameRes res = new LoginWithUsernameRes().setBanTime(-1).setDecisionType(3);
                //此时还应短信通知用户账号存在风险
                return new CommonResult<>(1, "已经15次密码错误，不再允许新的尝试", res);
            }

            //判断是不是异地登录
            if (authServiceImpl.checkRemoteLogin(req.getUsername(), ip, deviceId)) {
                return new CommonResult<>(1, "异地登录，请使用手机号登录");
            }

            //尝试进行登录
            String token = authServiceImpl.loginWithUsernameAfterCheck(req.getUsername(), ip, deviceId);
            //需要返回的一些信息（目前不清楚具体用途，先在这里随便写着）
            LoginWithUsernameRes res = new LoginWithUsernameRes()
                    .setSessionId(token)
                    .setExpireTime(JwtConstants.TIMEOUT)
                    .setDecisionType(0);
            return new CommonResult<>(0, "登录成功", res);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult<>(1, e.getMessage());
        }
    }

    @PostMapping("/loginWithPhone")
    public CommonResult<LoginWithPhoneRes> loginWithPhone(
            @Valid @RequestBody LoginWithPhoneReq req,
            @ApiParam(hidden = true) @RequestAttribute("ip") String ip,
            @ApiParam(hidden = true) @RequestAttribute("device_id") String deviceId) {
        try {
            //判断手机号是否存在（发验证码的时候其实也做过了）
            if (!authServiceImpl.checkPhoneExist(req.getPhoneNumber()))
                return new CommonResult<>(1, "手机号不存在");
            //判断手机验证码是否正确
            if (!verifyCodeServiceImpl.checkVerifyCode(req.getPhoneNumber(), req.getVerifyCode()))
                return new CommonResult<>(1, "验证码不正确");
            //尝试进行登录
            String token = authServiceImpl.loginWithPhoneAfterCheck(req.getPhoneNumber(), ip, deviceId);
            LoginWithPhoneRes res = new LoginWithPhoneRes()
                    .setSessionId(token)
                    .setExpireTime(JwtConstants.TIMEOUT);
            return new CommonResult<>(0, "登录成功", res);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult<>(1, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public CommonResult<Object> logout(@Valid @RequestBody LogoutReq req) {
        String currentUserId = SpringSecurityUtil.getCurrentUserId();
        try {
            if (req.getActionType().equals(1)) {
                String key = String.format(RedisConstants.LOGIN_STATE_KEY, currentUserId);
                redisTemplate.delete(key);
                return new CommonResult<>(0, "登出成功");
            } else if (req.getActionType().equals(2)) {
                Boolean res = authServiceImpl.logout(currentUserId);
                if (!res) return new CommonResult<>(1, "注销失败");
                return new CommonResult<>(0, "注销成功");
            }
            return new CommonResult<>(1, "不正确的actionType");
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult<>(1, e.getMessage());
        }
    }

    @ApiOperation("获取当前登录账号的所有登录记录")
    @PostMapping("/getLoginRecord")
    public CommonResult<List<Log>> getLoginRecord() {
        try {
            //提取信息
            String currentUserId = SpringSecurityUtil.getCurrentUserId();
            return new CommonResult<>(0, "请求成功", authServiceImpl.getLoginRecordById(currentUserId));
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult<>(1, e.getMessage());
        }
    }

    @ApiOperation("获取当前登录账号的信息")
    @PostMapping("/getUser")
    public CommonResult<User> getUser() {
        try {
            //提取信息
            String currentUserId = SpringSecurityUtil.getCurrentUserId();
            return new CommonResult<>(0, "请求成功", authServiceImpl.getUserById(currentUserId));
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult<>(1, e.getMessage());
        }
    }

    @ApiOperation("解除某个IP对某个账号的访问限制，删除相关的风控信息, 当成功解封时，返回1，否则返回0")
    @GetMapping("/delete/{username}/{ip}")
    public Integer delete(@PathVariable String username,
                          @PathVariable String ip) {
        System.out.println("解封：" + username + ip);

        String key = String.format("%s_request_count", ip);
        redisTemplate.delete(key);
        key = String.format("%s_block_count", ip);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove("ip_black_list", ip);
        key = String.format("%s_%s_wrong_pwd_count", username, ip);
        redisTemplate.delete(key);
        return 1;
    }

    @ApiOperation("检查token的有效性")
    @GetMapping("/checkToken")
    public Boolean checkToken(@RequestHeader("token") String token) {
        return true;
    }

    @ApiOperation("获取公钥，返回的字符串是经过Base64编码的公钥")
    @GetMapping("/getPublicKey")
    public CommonResult<String> getPublicKey() {
        RSA rsa = new RSA();
        String privateKeyBase64 = rsa.getPrivateKeyBase64();
        String publicKeyBase64 = rsa.getPublicKeyBase64();
        String redisKey = String.format(RedisConstants.PRIVATE_KEY_KEY, publicKeyBase64);
        redisTemplate.opsForValue().set(redisKey, privateKeyBase64, 3, TimeUnit.MINUTES);
        return CommonResult.ok(publicKeyBase64);
    }
}
