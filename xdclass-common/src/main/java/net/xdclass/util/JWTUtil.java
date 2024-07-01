package net.xdclass.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.model.LoginUser;

import java.util.Date;

/**
 * 设置JWT 工具类
 */
@Slf4j
public class JWTUtil {

    /**
     * token 过期时间， 正常为7天，方便测试为70天 TODO
     */
    private static final long EXPIRE = 1000 * 60 * 60 * 24 * 7 * 10;

    /**
     * 加密的 密钥
     */
    private static final String SECRET = "xdclass.net";

    /**
     * 令牌的前缀
     */
    private static final String TOKEN_PREFIX = "xdclass2014shop";

    /**
     * subject
     */
    private static final String SUBJECT = "xdclass";

    /**
     * 生成token
     *
     * @param loginUser
     * @return
     */
    public static String generateJWT(LoginUser loginUser) {
        if (loginUser == null) {
            throw new NullPointerException("没有LoginUser对象");
        }
        String token = Jwts.builder().setSubject(SUBJECT)
                // payload（负载）：主要描述是加密对象的信息，如用户的id等，也可以加些规范里面的东西，如iss签发者，exp 过期时间，sub 面向的用户
                .claim("head_img", loginUser.getHeadImg())
                .claim("id", loginUser.getId())
                .claim("name", loginUser.getName())
                .claim("mail", loginUser.getMail())
                .claim("ip", loginUser.getIp())
                // 设置发布时间
                .setIssuedAt(new Date())
                // 设置过期时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                // 设置签名的算法（head） & 密钥
                .signWith(SignatureAlgorithm.HS256, SECRET)
                // 完成
                .compact();

//        将token 拼接上一个 token 前缀，方便分辨
        token = TOKEN_PREFIX + token;
        return token;
    }

    /**
     * 校验token的方法
     * 利用移动端ip 判断是否token被泄露
     *
     * @param token
     * @return
     */
    public static Claims checkJWT(String token, String ip) {
        try {

            Claims claims = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();
            if (ip.equals(claims.get("ip").toString()))
                return claims;
            else throw new Exception();
        } catch (Exception e) {
            log.info("jwt token 解密失败");
            return null;
        }

    }

}
