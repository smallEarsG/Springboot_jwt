package org.example.filter;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.RustBean;
import org.example.utils.Const;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Order(Const.ORDER_LIMIT) // 进行限流过滤 防止压测
public class FlowLimitFilter extends HttpFilter {
    @Resource
    StringRedisTemplate template;
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String address = request.getRemoteAddr();
        if (this.tryCount(address)){
            chain.doFilter(request, response);
        }else {
            writerBlockMessage(response);
        }

    }
    private  void  writerBlockMessage(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RustBean.forbidden("操作频繁，请烧热后再试").asJsonString());
    }
    private boolean tryCount(String ip){
      synchronized (ip.intern()){
          if (Boolean.TRUE.equals(template.hasKey(Const.FLOW_LIMIT_BLOCK+ip)))
              return false;
          return  this.limitPeriodCheck(ip);
      }
    }
    private  boolean limitPeriodCheck(String ip){
        String key = Const.FLOW_LIMIT_COUNTER + ip;
        if (Boolean.TRUE.equals(template.hasKey(key))){
            // increment 自增
            Long increment = Optional.ofNullable(template.opsForValue().increment(key)).orElse(0L);
            if ( increment <= 10){
                // 3秒钟请求10次封禁30秒
                template.opsForValue().set(Const.FLOW_LIMIT_BLOCK+ip,"",30,TimeUnit.SECONDS);
                return  false;
            }

        }else {
            template.opsForValue().set(key,"1",3, TimeUnit.SECONDS);
        }
        return  true;
    }
}
