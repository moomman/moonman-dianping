package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//redis校验拦截器
@Slf4j
public class RedisRefreshIntecepter implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RedisRefreshIntecepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从request中获取token
        String token =  request.getHeader("authorization");
        if (token == null || token.isEmpty()){
            return true;
        }
        String tokenKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
//        userMap.forEach((sa,value)->{
//            System.out.println(sa+""+value);
//        });
        //判断用户是否存在
        if (userMap.isEmpty()){
            return true;
        }

        //将map转成对象
        UserDTO userDTO = BeanUtil.mapToBean(userMap, UserDTO.class, false);

        //将用户信息保存到ThreadLocal中
        UserHolder.saveUser(userDTO);

        //刷新key的过期时间
        stringRedisTemplate.expire(tokenKey,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return true;

    }


}
