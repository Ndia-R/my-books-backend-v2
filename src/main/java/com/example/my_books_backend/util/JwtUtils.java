package com.example.my_books_backend.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.my_books_backend.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtSecret}")
    private String secret;

    @Value("${spring.app.jwtAccessExpiration}")
    private int accessExpiration;

    @Value("${spring.app.jwtRefreshExpiration}")
    private int refreshExpiration;

    private static final String REFRESH_TOKEN_KEY = "refreshToken";

    /**
     * アクセストークン生成
     * 
     * @param user ユーザーエンティティ
     * @return アクセストークン
     */
    public String generateAccessToken(User user) {
        String email = user.getEmail();
        String name = user.getName();
        String roles = user.getRoles()
            .stream()
            .map(role -> role.getName().toString())
            .collect(Collectors.joining(","));

        return JWT.create()
            .withSubject(email)
            .withClaim("name", name)
            .withClaim("roles", roles)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + accessExpiration * 1000))
            .sign(getAlgorithm());
    }

    /**
     * リフレッシュトークン生成
     * 
     * @param user ユーザーエンティティ
     * @return リフレッシュトークン
     */
    public String generateRefreshToken(User user) {
        String email = user.getEmail();

        return JWT.create()
            .withSubject(email)
            .withIssuedAt(new Date())
            .withExpiresAt(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
            .sign(getAlgorithm());
    }

    /**
     * リフレッシュトークンからCookieを作成
     * 
     * @param refreshToken リフレッシュトークン
     * @return Cookie
     */
    public Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_KEY, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "None");
        cookie.setSecure(true);
        cookie.setMaxAge(refreshExpiration);
        return cookie;
    }

    /**
     * リフレッシュトークンを無効にしたCookieを取得
     * 
     * @return Cookie
     */
    public Cookie getInvalidateRefreshTokenCookie() {
        Cookie cookie = new Cookie(REFRESH_TOKEN_KEY, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "None");
        cookie.setSecure(true);
        cookie.setMaxAge(0); // すぐに削除
        return cookie;
    }

    /**
     * リフレッシュトークンをCookieから取得
     * 
     * @param request リクエスト
     * @return リフレッシュトークン
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_KEY.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * トークンの検証
     * 
     * @param token トークン
     * @return 検証結果
     */
    public boolean validateToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            logger.error("JWT検証エラー: {}", e.getMessage());
            return false;
        }
    }

    /**
     * トークンからサブジェクトを取得
     * 
     * @param token トークン
     * @return サブジェクト
     */
    public String getSubjectFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getSubject();
        } catch (JWTVerificationException e) {
            logger.error("JWT解析エラー: {}", e.getMessage());
            return null;
        }
    }

    /**
     * トークンからJTIを取得
     * 
     * @param token トークン
     * @return JTI
     */
    public String getJtiFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getId();
        } catch (JWTVerificationException e) {
            logger.error("JWT解析エラー: {}", e.getMessage());
            return null;
        }
    }

    /**
     * トークンから有効期限を取得
     * 
     * @param token トークン
     * @return 有効期限
     */
    public Date getExpiryTimeFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt();
        } catch (JWTVerificationException e) {
            logger.error("JWT解析エラー: {}", e.getMessage());
            return null;
        }
    }

    /**
     * トークンのロールを取得
     * 
     * @param token トークン
     * @return ロール
     */
    public List<String> getRolesFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            String rolesString = jwt.getClaim("roles").asString();
            return rolesString != null
                ? Arrays.asList(rolesString.split(","))
                : Collections.emptyList();
        } catch (JWTVerificationException e) {
            logger.error("JWT解析エラー: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Algorithmの取得
     * 
     * @return Algorithm
     */
    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secret);
    }
}

// ---------------------------------------------------------------------------
// JWT秘密鍵
// @Value("${spring.app.jwtSecret}")
// private String secret;
//
// 以下のようなプログラムで生成した値を環境変数に設定
// import java.security.SecureRandom;
// import java.util.Base64;
//
// public class JwtSecretGenerator {
// public static void main(String[] args) {
// // 256ビット（32バイト）のランダムな秘密鍵を生成
// byte[] keyBytes = new byte[32];
// new SecureRandom().nextBytes(keyBytes);
//
// // Base64エンコード
// String base64EncodedSecret = Base64.getEncoder().encodeToString(keyBytes);
//
// System.out.println("生成されたBase64エンコード秘密鍵:");
// System.out.println(base64EncodedSecret);
// }
// }
