package kr.kakaotech.community.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    @Value("${jwt.expirationtime.accessTtl}")
    private int accessTtlSec;
    @Value("${jwt.expirationtime.refreshTtl}")
    private int refreshTtlSec;

    public JwtProvider(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    // 검증
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
    public String getUserId(Claims claims) {
        return claims.getSubject();
    }
    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
    public boolean isExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
    public boolean isRefresh(Claims claims) {
        return claims.get("type").equals("refresh");
    }

    // 생성
    public String createAccess(String userId, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(accessTtlSec)))
                .signWith(secretKey)
                .compact();
    }

    public String createRefresh(String userId, String role) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("typ", "refresh")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(refreshTtlSec)))
                .signWith(secretKey)
                .compact();
    }
}
