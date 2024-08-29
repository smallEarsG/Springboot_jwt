package org.example.config;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.entity.RustBean;
import org.example.entity.dto.Account;
import org.example.entity.vo.response.AuthorizeVo;
import org.example.filter.JwtAuthorizeFilter;
import org.example.service.AccountService;
import org.example.utils.JwtUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;


@Configuration //标识一个类是配置类。配置类用于定义 Bean（对象）
public class SecurityConfiguration  {
    @Resource
    JwtUtils jwtUtils;
    @Resource
    JwtAuthorizeFilter jwtAuthorizeFilter;

    @Resource
    AccountService service;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {  // Security的过滤器链
        return  http
                .authorizeHttpRequests(conf -> conf
                        .requestMatchers("/api/auth/**","/error").permitAll() ///api/auth/允许放行 其他的需要进行校验
                        .anyRequest().authenticated())
                .formLogin(conf -> conf
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(this::onAuthenticationSuccess)
                        .failureHandler(this::onAuthenticationFailure))
                .logout(conf -> conf
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(this::onLogoutSuccess))

                .exceptionHandling(conf -> conf
                        /// 对于验证未通过的请求处理
                        .authenticationEntryPoint(this::onAuthenticationFailure)
                        // 登录了但是没权限的处理
                        .accessDeniedHandler(this::accessDeniedHandler))
                .csrf(AbstractHttpConfigurer::disable)
                // 修改session策略
                .sessionManagement(conf ->conf.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // jwt验证加在Security过滤器之前
                .addFilterBefore(jwtAuthorizeFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    public void accessDeniedHandler(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType("application/json;charset utf-8");
        response.getWriter().write(RustBean.forbidden(accessDeniedException.getMessage()).asJsonString());
    }
    private void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
       httpServletResponse.setContentType("application/json;charset utf-8");
        httpServletResponse.getWriter().write(RustBean.forbidden(e.getMessage()).asJsonString());
    }

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset utf-8");
        User user = (User) authentication.getPrincipal();// 可以拿到用户的详细信息
        // 这里的user已经通过了Security的loadUserByUsername的校验
        Account account = service.findAccountByNameOrEmail(user.getUsername());
        String token = jwtUtils.createJwt(user,1,account.getUsername());
//        AuthorizeVo vo = new AuthorizeVo();
//        BeanUtils.copyProperties(account,vo); // 拷贝account 的数据到Vo
//        vo.setExpire(jwtUtils.expiresTime());
//        vo.setToken(token);
//        vo.setRole(account.getRole());
//        vo.setUsername(account.getUsername());
        AuthorizeVo vo = account.aSViewObject(AuthorizeVo.class,v -> {
                v.setExpire(jwtUtils.expiresTime());
                v.setToken(token);
        });
        response.getWriter().write(RustBean.success(vo).asJsonString());

    }
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset utf-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if (jwtUtils.invalidateJwt(authorization)){
            writer.write(RustBean.success().asJsonString());
        }else {
            writer.write(RustBean.failure(401,"推出失败").asJsonString());
        }
    }

}
