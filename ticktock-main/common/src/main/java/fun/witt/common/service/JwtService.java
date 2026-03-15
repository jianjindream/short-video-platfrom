package fun.witt.common.service;

import fun.witt.common.auth.LoginUser;
import fun.witt.common.config.JwtProperties;
import fun.witt.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    @Autowired
    private JwtProperties jwtProperties;

    public String createAccessToken(User user) {
        return Jwts.builder()
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(user.getUserName())
                .claim("uid", user.getId())
                .claim("typ", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenValidityMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSigned())
                .compact();
    }

    public String createRefreshToken(User user, String jti) {
        return Jwts.builder()
                .setId(jti)
                .setIssuer(jwtProperties.getIssuer())
                .setSubject(user.getUserName())
                .claim("uid", user.getId())
                .claim("typ", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenValidityMs()))
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSigned())
                .compact();
    }

    public LoginUser parseAccessToken(String token) {
        Claims claims = parse(token);
        String type = claims.get("typ", String.class);
        if (!"access".equals(type)) {
            throw new IllegalArgumentException("token type error");
        }
        Long userId = Long.parseLong(String.valueOf(claims.get("uid")));
        return new LoginUser(userId, claims.getSubject());
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        String type = claims.get("typ", String.class);
        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("token type error");
        }
        return claims;
    }

    public long getAccessTokenValidityMs() {
        return jwtProperties.getAccessTokenValidityMs();
    }

    public long getRefreshTokenValidityMs() {
        return jwtProperties.getRefreshTokenValidityMs();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .setSigningKey(jwtProperties.getSigned())
                .parseClaimsJws(token)
                .getBody();
    }
}
