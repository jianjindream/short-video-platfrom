package fun.witt.gateway.security;

import fun.witt.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class GatewayJwtParser {

    private final GatewayJwtProperties jwtProperties;

    public GatewayJwtParser(GatewayJwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public GatewayUser parseAccessToken(String token) {
        JwtParser parser = Jwts.parser()
                .setSigningKey(jwtProperties.getSigned())
                .setAllowedClockSkewSeconds(jwtProperties.getClockSkewSeconds());
        if (StringUtils.hasText(jwtProperties.getIssuer())) {
            parser.requireIssuer(jwtProperties.getIssuer());
        }

        Claims claims = parser.parseClaimsJws(token).getBody();
        String type = claims.get("typ", String.class);
        if (!"access".equals(type)) {
            throw new IllegalArgumentException("token type error");
        }

        Object uid = claims.get("uid");
        if (uid == null) {
            throw new IllegalArgumentException("token uid missing");
        }
        Long userId = Long.parseLong(String.valueOf(uid));
        return new GatewayUser(userId, claims.getSubject());
    }
}
