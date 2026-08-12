package fun.witt.gateway.filter;

import fun.witt.gateway.config.GatewayAuthProperties;
import fun.witt.gateway.config.GatewayRateLimitProperties;
import fun.witt.gateway.ratelimit.RedisSlidingWindowRateLimiter;
import fun.witt.gateway.security.GatewayJwtParser;
import fun.witt.gateway.security.GatewayUser;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class GatewaySecurityGlobalFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_ATTR = "short-video-platfrom.gateway.userId";

    private final GatewayAuthProperties authProperties;

    private final GatewayRateLimitProperties rateLimitProperties;

    private final GatewayJwtParser jwtParser;

    private final RedisSlidingWindowRateLimiter rateLimiter;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public GatewaySecurityGlobalFilter(GatewayAuthProperties authProperties,
                                       GatewayRateLimitProperties rateLimitProperties,
                                       GatewayJwtParser jwtParser,
                                       RedisSlidingWindowRateLimiter rateLimiter) {
        this.authProperties = authProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.jwtParser = jwtParser;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(authProperties.getUserIdHeader());
                    headers.remove(authProperties.getUsernameHeader());
                })
                .build();
        ServerWebExchange cleanExchange = exchange.mutate().request(cleanRequest).build();

        return authenticate(cleanExchange)
                .flatMap(authenticatedExchange -> rateLimit(authenticatedExchange, chain));
    }

    private Mono<ServerWebExchange> authenticate(ServerWebExchange exchange) {
        if (!authProperties.isEnabled()) {
            return Mono.just(exchange);
        }

        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        boolean publicPath = isPublicPath(path);
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        boolean hasBearer = StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ");

        if (!hasBearer) {
            if (publicPath) {
                return Mono.just(exchange);
            }
            return writeJson(exchange.getResponse(), HttpStatus.UNAUTHORIZED,
                    "{\"status_code\":1,\"status_msg\":\"authorization required\"}")
                    .then(Mono.empty());
        }

        try {
            GatewayUser user = jwtParser.parseAccessToken(authHeader.substring(7));
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.set(authProperties.getUserIdHeader(), String.valueOf(user.getUserId()));
                        if (StringUtils.hasText(user.getUsername())) {
                            headers.set(authProperties.getUsernameHeader(), user.getUsername());
                        }
                    })
                    .build();
            ServerWebExchange authenticated = exchange.mutate().request(request).build();
            authenticated.getAttributes().put(USER_ID_ATTR, String.valueOf(user.getUserId()));
            return Mono.just(authenticated);
        } catch (Exception ex) {
            return writeJson(exchange.getResponse(), HttpStatus.UNAUTHORIZED,
                    "{\"status_code\":1,\"status_msg\":\"token invalid or expired\"}")
                    .then(Mono.empty());
        }
    }

    private Mono<Void> rateLimit(ServerWebExchange exchange, GatewayFilterChain chain) {
        GatewayRateLimitProperties.Rule rule = findRateLimitRule(exchange);
        if (!rateLimitProperties.isEnabled() || rule == null) {
            return chain.filter(exchange);
        }

        String identity = resolveIdentity(exchange, rule);
        String key = rateLimitProperties.getKeyPrefix() + ":" + rule.getId() + ":" + identity;
        long windowMillis = rule.getWindowSeconds() * 1000L;
        return rateLimiter.isAllowed(key, rule.getLimit(), windowMillis)
                .flatMap(allowed -> {
                    if (Boolean.TRUE.equals(allowed)) {
                        return chain.filter(exchange);
                    }
                    return writeJson(exchange.getResponse(), HttpStatus.TOO_MANY_REQUESTS,
                            "{\"status_code\":1,\"status_msg\":\"too many requests\"}");
                })
                .onErrorResume(ex -> chain.filter(exchange));
    }

    private GatewayRateLimitProperties.Rule findRateLimitRule(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        HttpMethod method = exchange.getRequest().getMethod();
        List<GatewayRateLimitProperties.Rule> rules = rateLimitProperties.getRules();
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (GatewayRateLimitProperties.Rule rule : rules) {
            if (rule.getLimit() <= 0 || rule.getWindowSeconds() <= 0 || !StringUtils.hasText(rule.getPath())) {
                continue;
            }
            if (!pathMatcher.match(rule.getPath(), path)) {
                continue;
            }
            if (rule.getMethods() == null || rule.getMethods().isEmpty()) {
                return rule;
            }
            if (method != null && containsMethod(rule.getMethods(), method.name())) {
                return rule;
            }
        }
        return null;
    }

    private boolean containsMethod(List<String> methods, String method) {
        for (String candidate : methods) {
            if (method.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String resolveIdentity(ServerWebExchange exchange, GatewayRateLimitProperties.Rule rule) {
        if (GatewayRateLimitProperties.KeyBy.USER.equals(rule.getKeyBy())) {
            Object userId = exchange.getAttribute(USER_ID_ATTR);
            if (userId != null) {
                return "user:" + userId;
            }
        }
        return "ip:" + resolveClientIp(exchange);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : authProperties.getPublicPaths()) {
            if (pathMatcher.match(publicPath, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> writeJson(ServerHttpResponse response, HttpStatus status, String body) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
