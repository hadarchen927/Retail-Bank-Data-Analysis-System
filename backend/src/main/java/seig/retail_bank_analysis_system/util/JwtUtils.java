// backend/src/main/java/seig/retail_bank_analysis_system/util/JwtUtils.java
package seig.retail_bank_analysis_system.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    // 密钥（至少256位）
    private static final String SECRET_KEY = "retail-bank-analysis-system-secret-key-must-be-at-least-256-bits-long";

    // 生成密钥对象
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    // Token 有效期（7天）
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    /**
     * 生成 Token
     */
    public static String createToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }

    /**
     * 从 Token 中获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 验证 Token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}