package org.example.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class FlowUtils { // 对邮箱发送进行限流 n分钟只能发一次
    @Resource
    StringRedisTemplate template;
    public boolean limitOncCheck(String key, int blockTime){
        if (Boolean.TRUE.equals(template.hasKey(key))){  // 如果redis存这个key 则代表在规定时间内
            return  false;
        }else {
            // 不存在则发送创建一个过期时间
            template.opsForValue().set(key,"",blockTime, TimeUnit.SECONDS);
            return  true;
        }
    }
}
