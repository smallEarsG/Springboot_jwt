package org.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Account;
import org.example.entity.vo.request.ConfirmResetVo;
import org.example.entity.vo.request.EmailRegisterVo;
import org.example.entity.vo.request.EmailResetVo;
import org.example.mapper.AccountMapper;
import org.example.service.AccountService;
import org.example.utils.Const;
import org.example.utils.FlowUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.print.DocFlavor;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {
    @Resource
    AmqpTemplate amqpTemplate;
    @Resource
    StringRedisTemplate redisTemplate;
    @Resource
    FlowUtils flowUtils;

    @Resource
    PasswordEncoder passwordEncoder;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())  // Security将会用用户输入的密码和UserDetails中的password比较
                .roles(account.getRole())
                .build();
    }

    public Account findAccountByNameOrEmail(String text){
        return this.query()
                .eq("username",text)
                .or()
                .eq("email",text)
                .one();
    }

    @Override
    public String registerEmailVerifyCode(String type, String email, String ip) {
        /// 防止压测
        synchronized (ip.intern()){ // 利用intern将ip常量话 保证同一个时刻只有 相同的ip在执行synchronized代码块
            if (!this.verifyLimit(ip))
                return "请求频繁 请稍后再试";
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;  // 保证随机数一直是6位
            Map<String, Object> data = Map.of("type",type,"email",email,"code",code);
            amqpTemplate.convertAndSend("email",data); // 将要发送的邮件信息存入队列
            // 将验证码存入redis等用输入时进行校验
            redisTemplate.opsForValue().set(Const.VERIFY_EMAIL_DATA+email,String.valueOf(code),3, TimeUnit.MINUTES);  // 3分钟有效 3分钟redis自动删除验证码
            return null;
        }

    }
    @Override
    public String registerEmailAccount(EmailRegisterVo vo){
        String email = vo.getEmail();
        String key = Const.VERIFY_EMAIL_DATA+email;
        String code  = redisTemplate.opsForValue().get(key);
        String username = vo.getUsername();
        if (code == null) return  "请先获取验证码";
        if (code.equals(vo.getCode()))return "验证码输入错误， 请重新输入";
        if (existsAccountByEmail(email))return "此邮箱已被其他用户注册";
        if (existsAccountByUsername(username))return "此用户名以被注册请更换一个新的";
        String password = passwordEncoder.encode(vo.getPassword());
        Account account = new Account(null, username, password,email,"user",new Date());
        if (this.save(account)){
            redisTemplate.delete(key);
            return null;
        }else {
            return "内部错误 请联系管理员";
        }


    }

    @Override
    public String resetConfirm(ConfirmResetVo vo) {
        String email = vo.getEmail();
        String code = redisTemplate.opsForValue().get(Const.VERIFY_EMAIL_DATA + email);
        if (code == null ) return  "请先获取验证码";
        if (!code.equals(vo.getCode())) return "验证码错误， 请重新输入";
        return null;
    }

    @Override
    public String resetEmailAccountPassword(EmailResetVo vo) {
        String email = vo.getEmail();
        String verify = this.resetConfirm(new ConfirmResetVo(email,vo.getCode()));
        if (verify != null) return  verify;
        String password = passwordEncoder.encode(vo.getPassword());
        boolean update = this.update().eq("email",email).set("password",password).update();
        if (update){
            redisTemplate.delete(Const.VERIFY_EMAIL_DATA+email);
        }
        return null;
    }

    private  boolean existsAccountByEmail(String email){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }
    private  boolean existsAccountByUsername(String username){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username",username));
    }
    private  boolean verifyLimit(String ip){ // 根据ip限制请求
        String key = Const.VERIFY_EMAIL_LIMIT + ip;
        return flowUtils.limitOncCheck(key,60);
    }

}
