package fun.witt.gateway.security;

public class GatewayUser {

    private final Long userId;

    private final String username;

    public GatewayUser(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
