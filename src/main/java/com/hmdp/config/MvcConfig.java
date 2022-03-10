package com.hmdp.config;


import com.hmdp.utils.LoginIntecepter;
import com.hmdp.utils.RedisRefreshIntecepter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//mvc配置类
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //判断用户是否存在
        registry.addInterceptor(new LoginIntecepter())
                .excludePathPatterns(
                        "/user/code",
                        "/user/login",
                        "/shop/**",
                        "/shop-type/**",
                        "/blog/hot"
                );

        //在访问全部路径时都刷新tokenkey的存活时间,避免突然退出登录的情况
        registry.addInterceptor(new RedisRefreshIntecepter(stringRedisTemplate)).addPathPatterns("/**");
    }
}


