package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
//验证码
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result SendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);

        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
//存储到Redis
//        session.setAttribute("code",code);
        log.info("验证码发送{}",code);

        return Result.ok();
    }
//登录
    @Override
    public Result Login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();

        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //验证
        String  code   = loginForm.getCode();
        String cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        if (cachecode==null||!cachecode.equals(code)){
            return Result.fail("验证码错误");
        }
        //MY-plus
        User user = query().eq("phone",phone).one();
        if (user==null){
            log.info("用户不存在，创建新用户: {}", phone);
          user  = createUserWithPhone(phone);
        }
        log.info("用户存在: {}", phone);

        String token = UUID.randomUUID().toString(true);
//        Map<String, Object> usermap = BeanUtil.beanToMap(user);
        String tokenkey = LOGIN_USER_KEY+token;
        Map<String, String> usermap = new HashMap<>();
        usermap.put("id", user.getId().toString());
        usermap.put("nickName", user.getNickName());
        usermap.put("icon", user.getIcon());
        stringRedisTemplate.opsForHash().putAll(tokenkey,usermap);
        stringRedisTemplate.expire(tokenkey,CACHE_SHOP_TTL,TimeUnit.MINUTES);



//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User  user = new User();
        user.setPhone(phone);
        user.setNickName("user_"+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
