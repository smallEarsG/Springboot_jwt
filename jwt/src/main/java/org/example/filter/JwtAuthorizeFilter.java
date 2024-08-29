package org.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.utils.JwtUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthorizeFilter extends OncePerRequestFilter { // 在Security的过滤器链中添加过滤器
    @Resource
    JwtUtils utils;

    @Override // 每次请求filter都会执行一次
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        DecodedJWT jwt = utils.resolveJwt(authorization);  // jwt验证
        if (jwt !=null){
            UserDetails user  = utils.toUser(jwt);
            // 创建Security的用户校验对象
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
//            认证令牌的详细信息。WebAuthenticationDetailsSource 用于从 HTTP 请求中提取与认证相关的详细信息，比如用户的 IP 地址、会话 ID 等。将这些详细信息添加到认证令牌中可以在后续的安全处理中使用，例如审计、日志记录或更精细的访问控制决策。
          // 如果不需要可以去去掉
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 添加到Security上下午进行解析
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            // 如果在请求中需要某些参数可以在request中添加
            request.setAttribute("id",utils.toId(jwt));

        }
    }

}
