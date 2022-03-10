package com.hmdp.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 *
 *
 * 更改为redis来实现
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误,请重新输入");
        }

        //生成验证码(6位)
        String s = RandomUtil.randomNumbers(6);

//        //将验证码保存到session中
//        session.setAttribute("verificationCode",s);
//        session.setAttribute("phoneNumber",phone);

        //将session中的信息转到redis中,以(手机号:验证码)的KV键值对存储
        String phoneKey = LOGIN_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(phoneKey,s);

        //设置验证码的过期时间
        stringRedisTemplate.expire(phoneKey,LOGIN_CODE_TTL,TimeUnit.MINUTES);
        //发送验证码
        log.info("验证码{}已发送",s);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //验证手机号和验证码
        log.info(loginForm.toString());
        String phone = loginForm.getPhone();
        if (phone == null || RegexUtils.isPhoneInvalid(phone) || stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone) == null){
            return Result.fail("手机号或验证码错误");
        }

        String code = loginForm.getCode();

        if (code == null || !code.equals(stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone))){
            return Result.fail("手机号或验证码错误");
        }
        //根据手机号查询该用户是否存在
        User user = query().eq("phone", phone).one();

        //如果用户不存在则创建新用户
        if (user == null){
            User newUser = createNewUser(phone);
            if (newUser == null){
                return Result.fail("用户保存失败");
            }else{
                return Result.ok();
            }
        }
//
//        //保存到session中
//        session.setAttribute("userMsg",user);
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        HashMap<String,Object>userMap = (HashMap<String, Object>) BeanUtil.beanToMap(userDTO,
                new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                .setFieldValueEditor((fieldName,fieldValue)-> fieldValue.toString())
                );

        //生成随机字符串当做token
        String token = UUID.randomUUID().toString();
        //保存在redis中
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);

        //设置token的过期时间
        stringRedisTemplate.expire(tokenKey,CACHE_SHOP_TTL,TimeUnit.MINUTES);
        return Result.ok(token);//相当于把token传给了用户
    }


    //创建新用户
    private User createNewUser(String phone){
        User user = new User();
        user.setPhone(phone);
        user.setNickName("User_"+RandomUtil.randomString(10));
        user.setCreateTime(LocalDateTime.now());

        boolean save = save(user);
        if (save){
            return user;
        }else{
            return null;
        }
    }
}
