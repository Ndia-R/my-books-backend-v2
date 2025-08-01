package com.example.my_books_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @Schema(description = "メールアドレス", example = "user@example.com", required = true)
    @NotNull
    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    @Schema(description = "パスワード", example = "passward123", required = true)
    @NotNull
    @NotBlank(message = "パスワードは必須です")
    @Size(min = 3, message = "パスワードは3文字以上で入力してください")
    private String password;

    @Schema(description = "ユーザー名", example = "ユーザー", required = true)
    @NotNull
    @NotBlank(message = "ユーザー名は必須です")
    private String name;

    @Schema(description = "アバター画像のパス", example = "/avatar01.png", required = true)
    @NotNull
    @NotBlank(message = "アバターは必須です")
    private String avatarPath;
}
