package com.example.my_books_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenResponse {

    @Schema(description = "アクセストークン", example = "eyJhbGciOiJIUzI1NiIs...")
    private String accessToken;
}
