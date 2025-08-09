package com.example.my_books_backend.config;

import java.util.List;
import java.util.Arrays;
import org.springframework.stereotype.Component;

@Component
public class SecurityEndpointsConfig {

    // 完全にパブリックなエンドポイント
    public List<String> getFullyPublicEndpoints() {
        return Arrays.asList(
            "/login",
            "/signup",
            "/logout",
            "/refresh-token",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/health"
        );
    }

    // GETメソッドのみパブリックなエンドポイント
    public List<String> getPublicGetEndpoints() {
        return Arrays.asList(
            "/genres/**",
            "/books/**"
        );
    }
}
