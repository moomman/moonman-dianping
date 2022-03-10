package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//拦截器
public class LoginIntecepter implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public LoginIntecepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从request中获取token
        String token = (String) request.getAttribute("authorization");
        if (token == null || token.isEmpty()){
            return false;
        }
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        //判断用户是否存在
        if (userMap.isEmpty()){
            return false;
        }

        //将map转成对象
        UserDTO userDTO = BeanUtil.mapToBean(userMap, UserDTO.class, false);

        //将用户信息保存到ThreadLocal中
        UserHolder.saveUser(userDTO);

        //刷新key的过期时间
        stringRedisTemplate.expire(tokenKey,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
