package org.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {
    private final String KEY = "guozhijing" ;
    private final int EXPIRE = 7 ;

    @Resource
    StringRedisTemplate redisTemplate;
    public String createJwt(UserDetails details,int id,String username){
        Algorithm algorithm  = Algorithm.HMAC256(KEY);  // 加密的算法
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withClaim("id",id)
                .withClaim("name",username)
                .withClaim("authorities",details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .withExpiresAt(expiresTime())  // 过期时间
                .withIssuedAt(new Date())  // token颁发时间
                .sign(algorithm);
    }
    public Date expiresTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR,EXPIRE*24);
        return calendar.getTime();
    }
// 解析
    public DecodedJWT resolveJwt(String headerToken){
        String token = convertToken(headerToken);
        if (token == null) return  null;
        Algorithm algorithm = Algorithm.HMAC256(KEY);  // 定义加密算法
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();  // 生成校验器
        try {
            // 验证令牌是否被用户篡改
            DecodedJWT verify = jwtVerifier.verify(token); // 验证通过返回jwt
            //验证令牌是否过期
            Date expiresAt = verify.getExpiresAt();
            return  new Date().after(expiresAt)?null :verify;
        }catch (JWTVerificationException e){
            // 验证失败抛出异常 （运行时异常不会终端运行）
            return  null;
        }


    }
    public  Integer toId(DecodedJWT jwt){
        Map<String, Claim> claim = jwt.getClaims();
        return claim.get("id").asInt();
    }
    private String convertToken(String headerToken){
        if (headerToken == null || !headerToken.startsWith("Bearer")) return null;
        return headerToken.substring(7);
    }
    public  UserDetails toUser(DecodedJWT jwt){
        Map<String, Claim> claim = jwt.getClaims();
        return User
                .withUsername(claim.get("name").asString())
                .password("******")
                .authorities(claim.get("authorities").asArray(String.class))
                .build();

    }
    public boolean invalidateJwt(String headerToken){
        String token = convertToken(headerToken);
        if (token == null) return  false;
        Algorithm algorithm = Algorithm.HMAC256(KEY);
        JWTVerifier  jwtVerifier = JWT.require(algorithm).build();
        try{
            DecodedJWT jwt = jwtVerifier.verify(token);
            String id = jwt.getId();
            return  deleteToken(id,jwt.getExpiresAt());
        }catch (JWTVerificationException e){
            return false;
        }
    }
    private boolean deleteToken(String uuid,Date time){
        if (this.isInvalidToken(uuid)) return  false;
        Date now = new Date();
        long expire = Math.max(time.getTime()-now.getTime(),0);
        redisTemplate.opsForValue().set(Const.JWT_BLACK_LIST+uuid,"",expire);
        return  true;
    }
    //判断令牌是否已经过期
    private boolean isInvalidToken(String uuid){
        return  Boolean.TRUE.equals(redisTemplate.hasKey(Const.JWT_BLACK_LIST+uuid));
    }
}
