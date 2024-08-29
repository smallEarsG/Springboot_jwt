package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.utils.Const;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Const.ORDER_CORS) // 设置跨域优先级 这个必须在Security的过滤器之前
public class CorsFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request,response);
        chain.doFilter(request, response);
    }
    private  void  addCorsHeader(HttpServletRequest request, HttpServletResponse response){
         // 允许的请求地址  request.getHeader("Origin") 表示任何都可以 指定http://localhost:5173
         response.addHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
         // 允许的请求方法
         response.addHeader("Access-Control-Allow-Method","GET,POST,PUT,DELETE,POTIONS,PATCH");
         // 允许的请求头
         response.addHeader("Access-Control-Allow-Headers","Authorization,Content-Type");
    }
}
