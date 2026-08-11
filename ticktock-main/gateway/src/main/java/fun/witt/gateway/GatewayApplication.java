package fun.witt.gateway;

import fun.witt.gateway.config.GatewayAuthProperties;
import fun.witt.gateway.config.GatewayJwtProperties;
import fun.witt.gateway.config.GatewayRateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@EnableConfigurationProperties({
        GatewayAuthProperties.class,
        GatewayJwtProperties.class,
        GatewayRateLimitProperties.class
})
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
