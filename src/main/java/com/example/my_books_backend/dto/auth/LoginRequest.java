package com.example.my_books_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(description = "メールアドレス", example = "julia@gmail.com", required = true)
    @NotNull
    @Email(message = "無効なメールアドレスです")
    private String email;

    @Schema(description = "パスワード", example = "abc", required = true)
    @NotNull
    private String password;
}
