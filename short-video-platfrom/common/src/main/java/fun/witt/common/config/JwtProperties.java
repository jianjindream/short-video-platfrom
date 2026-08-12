package fun.witt.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String signed;
    private String issuer;
    private long accessTokenValidityMs = 30 * 60 * 1000L;
    private long refreshTokenValidityMs = 7 * 24 * 60 * 60 * 1000L;
}
