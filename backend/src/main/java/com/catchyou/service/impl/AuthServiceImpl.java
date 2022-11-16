package com.catchyou.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.catchyou.constant.JwtConstants;
import com.catchyou.constant.RedisConstants;
import com.catchyou.constant.UserType;
import com.catchyou.dao.LogMapper;
import com.catchyou.dao.UserMapper;
import com.catchyou.pojo.Log;
import com.catchyou.pojo.User;
import com.catchyou.pojo.dto.LoginUser;
import com.catchyou.service.AuthService;
import com.catchyou.util.MyUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LogMapper logMapper;

    /**
     * 生成JWT Token
     * @param userId 用户ID
     * @return 生成的JWT Token
     */
    private String generateToken(String userId) {
        Map<String, Object> payload = new HashMap<>();
        // 在JWT Token中存储用户的ID
        payload.put("userId", userId);
        // 设置JWT Token的过期时间
        payload.put(JWTPayload.EXPIRES_AT, new Date(System.currentTimeMillis() + JwtConstants.TIMEOUT));
        return JWTUtil.createToken(payload, JwtConstants.JWT_KEY);
    }

    /**
     * 保存用户的登录状态到Redis中，
     * 登录状态里保存的信息包括token、用户ID、用户名、用户类型，
     * 在Redis中，以用户ID为键保存登录状态
     * @param token JWT Token
     * @param userId 用户ID
     * @param username 用户名
     * @param userType 用户类型
     */
    private void saveLoginState(String token, String userId, String username, UserType userType) {
        String loginStateKey = String.format(RedisConstants.LOGIN_STATE_KEY, userId);
        redisTemplate.opsForHash().put(loginStateKey, "token", token);
        LoginUser loginUser = new LoginUser(userId, username, userType);
        redisTemplate.opsForHash().put(loginStateKey, "login_user", JSONUtil.toJsonStr(loginUser));
    }

    @Override
    //判断用户名是否已经存在
    public Boolean checkUsernameExist(String username) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getUsername, username).eq(User::getIsActive, 1);
        User user = userMapper.selectOne(cond);
        return user != null;
    }

    @Override
    public Boolean checkPhoneExist(String phone) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getPhoneNumber, phone).eq(User::getIsActive, 1);
        User user = userMapper.selectOne(cond);
        return user != null;
    }

    @Override
    public String registerAfterCheck(User user) {
        //密码需要加密
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        //为用户设置一个uuid
        String uuid = UUID.randomUUID().toString();
        user.setId(uuid);
        //插入到数据库中
        System.out.println(user);
        int res = userMapper.insert(user);
        if (res == 0) {
            //插入失败返回null
            return null;
        }
        //登录记录
        Log log = new Log(null, user.getId(), new Date(), user.getRegisterIp(), user.getRegisterDeviceId());
        logMapper.insert(log);
        //风控信息
        String key = user.getUsername() + "_login_cities";
        redisTemplate.opsForSet().add(key, MyUtil.getCityFromIp(user.getRegisterIp()));
        key = user.getUsername() + "_login_devices";
        redisTemplate.opsForSet().add(key, user.getRegisterDeviceId());
        String token = generateToken(uuid);
        saveLoginState(token, uuid, user.getUsername(), user.getType());
        //插入成功返回token
        return token;
    }

    @Override
    public String loginWithUsernameAfterCheck(String username, String ip, String deviceId) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getUsername, username).eq(User::getIsActive, true);
        User user = userMapper.selectOne(cond);
        //登录记录
        Log log = new Log(null, user.getId(), new Date(), ip, deviceId);
        logMapper.insert(log);
        //风控信息
        String key = username + "_login_cities";
        redisTemplate.opsForSet().add(key, MyUtil.getCityFromIp(ip));
        key = username + "_login_devices";
        redisTemplate.opsForSet().add(key, deviceId);
        String token = generateToken(user.getId());
        saveLoginState(token, user.getId(), user.getUsername(), user.getType());
        return token;
    }

    @Override
    public String loginWithPhoneAfterCheck(String phone, String ip, String deviceId) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getPhoneNumber, phone).eq(User::getIsActive, true);
        User user = userMapper.selectOne(cond);
        //登录记录
        Log log = new Log(null, user.getId(), new Date(), ip, deviceId);
        logMapper.insert(log);
        //风控信息
        String key = user.getUsername() + "_login_cities";
        redisTemplate.opsForSet().add(key, MyUtil.getCityFromIp(ip));
        key = user.getUsername() + "_login_devices";
        redisTemplate.opsForSet().add(key, deviceId);
        String token = generateToken(user.getId());
        saveLoginState(token, user.getId(), user.getUsername(), user.getType());
        return token;
    }

    @Override
    public Boolean logout(String uid) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getId, uid).eq(User::getIsActive, 1);
        User user = userMapper.selectOne(cond);
        if (user == null) {
            return false;
        }
        LambdaUpdateWrapper<User> cond2 = new LambdaUpdateWrapper<>();
        cond2.eq(User::getId, user.getId()).set(User::getIsActive, 0);
        userMapper.update(user, cond2);
        //一些风控信息的清除
        String key = user.getUsername() + "_login_cities";
        redisTemplate.delete(key);
        key = user.getUsername() + "_login_devices";
        redisTemplate.delete(key);
        return true;
    }

    @Override
    public List<Log> getLoginRecordById(String uid) {
        LambdaQueryWrapper<Log> cond = new LambdaQueryWrapper<>();
        cond.eq(Log::getUid, uid);
        return logMapper.selectList(cond);
    }

    //返回 0 表示匹配成功
    //返回 1 表示用户名不存在，无惩罚机制
    //返回 2 表示匹配失败，无惩罚机制
    //返回 3 表示匹配失败，用户1分钟内无法再尝试（针对于某个ip地址）
    //返回 4 表示匹配失败，用户5分钟内无法再尝试（针对于某个ip地址）
    //返回 5 表示匹配失败，禁止用户登录（针对于某个ip地址）
    @Override
    public Integer checkUsernamePasswordMatch(String username, String password, String ip) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getUsername, username).eq(User::getIsActive, 1);
        User user = userMapper.selectOne(cond);
        if (user == null) {
            return 1;
        }
        String key = null;
        if (!BCrypt.checkpw(password, user.getPassword())) {
            key = username + "_" + ip +
                    "_wrong_pwd_count";
            Boolean redisData = redisTemplate.hasKey(key);
            Assert.notNull(redisData, "Redis获取数据异常");
            if (!redisData) {
                redisTemplate.opsForValue().set(key, "1");
            } else {
                redisTemplate.opsForValue().increment(key);
            }
            String redisData2 = redisTemplate.opsForValue().get(key);
            Assert.notNull(redisData2, "Redis获取数据异常");
            int count = Integer.parseInt(redisData2);
            //如果错了5次，那么1分钟内不允许用户再尝试
            if (count == 5) {
                return 3;
            }
            //如果错了10次，那么5分钟内不允许用户再尝试
            if (count == 10) {
                return 4;
            }
            //如果错了15次，那么封号处理（针对这个ip的封号）
            if (count == 15) {
                return 5;
            }
            return 2;
        }
        //一旦登录成功，那么需要把风控信息清除
        if (key != null) {
            redisTemplate.delete(key);
        }
        return 0;
    }

    @Override
    public User getUserById(String uid) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getId, uid);
        return userMapper.selectOne(cond);
    }

    @Override
    //规定，一个设备最多只能注册五个账号
    public Boolean checkRubbishRegister(String deviceId) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getRegisterDeviceId, deviceId);
        Integer count = userMapper.selectCount(cond);
        Assert.notNull(count, "MySQL获取数据异常");
        return count >= 5;
    }

    @Override
    //如果使用密码登录，那么需要进行异地检测
    public Boolean checkRemoteLogin(String username, String ip, String deviceId) {
        String key = username + "_login_devices";
        //如果不是信任的设备才需要检测
        Boolean redisData = redisTemplate.opsForSet().isMember(key, deviceId);
        Assert.notNull(redisData, "Redis获取数据异常");
        if (!redisData) {
            String city = MyUtil.getCityFromIp(ip);
            key = username + "_login_cities";
            //如果不是信任的ip地址
            redisData = redisTemplate.opsForSet().isMember(key, city);
            Assert.notNull(redisData, "Redis获取数据异常");
            return !redisData;
        }
        return false;
    }
}
