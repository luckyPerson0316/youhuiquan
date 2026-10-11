package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    StringRedisTemplate stringRedisTemplate;
    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }



    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//获取token
        String token = request.getHeader("authorization");
        String key = LOGIN_USER_KEY+token;
        //根据token获取Redis中的用户
        Map<Object, Object> usermap = stringRedisTemplate.opsForHash().entries(key);
//判断是否为空
        if (usermap.isEmpty()){
            return true;
        }
//map转为对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(usermap, new UserDTO(), false);
        //存入ThreadLocal
        UserHolder.saveUser((UserDTO) userDTO);

    //刷新token时间
        stringRedisTemplate.expire(key,CACHE_SHOP_TTL, TimeUnit.MINUTES);


        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
