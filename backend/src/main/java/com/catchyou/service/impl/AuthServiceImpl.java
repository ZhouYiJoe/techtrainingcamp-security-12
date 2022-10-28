package com.catchyou.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.catchyou.dao.LogMapper;
import com.catchyou.dao.UserMapper;
import com.catchyou.pojo.Log;
import com.catchyou.pojo.User;
import com.catchyou.service.AuthService;
import com.catchyou.util.MyUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LogMapper logMapper;

    @Override
    //判断用户名是否已经存在
    public Boolean checkUsernameExist(String username) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getUsername, username);
        User user = userMapper.selectOne(cond);
        return user != null;
    }

    @Override
    public Boolean checkPhoneExist(String phone) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getPhoneNumber, phone);
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
        //插入成功返回uuid
        return uuid;
    }

    @Override
    //返回 0 表示匹配成功
    //返回 1 表示用户名不存在，无惩罚机制
    //返回 2 表示匹配失败，无惩罚机制
    //返回 3 表示匹配失败，用户1分钟内无法再尝试（针对于某个ip地址）
    //返回 4 表示匹配失败，用户5分钟内无法再尝试（针对于某个ip地址）
    //返回 5 表示匹配失败，禁止用户登录（针对于某个ip地址）
    public Integer checkUsernamePasswordMatch(String username, String password, String ip) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getUsername, username);
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
    public String loginWithUsernameAfterCheck(String username, String ip, String deviceId) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getUsername, username);
        User user = userMapper.selectOne(cond);
        //登录记录
        Log log = new Log(null, user.getId(), new Date(), ip, deviceId);
        logMapper.insert(log);
        //风控信息
        String key = username + "_login_cities";
        redisTemplate.opsForSet().add(key, MyUtil.getCityFromIp(ip));
        key = username + "_login_devices";
        redisTemplate.opsForSet().add(key, deviceId);
        return user.getId();
    }

    @Override
    public String loginWithPhoneAfterCheck(String phone, String ip, String deviceId) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getPhoneNumber, phone);
        User user = userMapper.selectOne(cond);
        //登录记录
        Log log = new Log(null, user.getId(), new Date(), ip, deviceId);
        logMapper.insert(log);
        //风控信息
        String key = user.getUsername() + "_login_cities";
        redisTemplate.opsForSet().add(key, MyUtil.getCityFromIp(ip));
        key = user.getUsername() + "_login_devices";
        redisTemplate.opsForSet().add(key, deviceId);
        return user.getId();
    }

    @Override
    public Boolean logout(String uid) {
        LambdaQueryWrapper<User> cond = new LambdaQueryWrapper<>();
        cond.eq(User::getId, uid);
        User user = userMapper.selectOne(cond);
        if (user == null) {
            return false;
        }
        LambdaUpdateWrapper<User> cond2 = new LambdaUpdateWrapper<>();
        cond2.eq(User::getId, user.getId()).set(User::getIsActive, false);
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
        cond.eq(Log::getId, uid);
        return logMapper.selectList(cond);
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
