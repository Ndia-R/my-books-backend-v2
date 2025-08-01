package com.example.my_books_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.my_books_backend.service.impl.UserDetailsServiceImpl;
import com.example.my_books_backend.util.JwtUtils;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtil;
    private final SecurityEndpointsConfig securityEndpointsConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    )
        throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String requestId = UUID.randomUUID().toString().substring(0, 8);

        // パブリックエンドポイントのチェック
        if (isPublicEndpoint(requestURI, method)) {
            logger.debug("[{}] パブリックエンドポイントへのアクセス: {} {}", requestId, method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // 認証が必要なエンドポイントの処理
        boolean authenticationSuccessful = false;
        try {
            authenticationSuccessful = processAuthentication(request, requestId);
        } catch (Exception e) {
            logger.error(
                "[{}] 認証処理中に予期しないエラーが発生: {} {}",
                requestId,
                e.getClass().getSimpleName(),
                e.getMessage()
            );
            handleAuthenticationError(response, requestId, "認証処理でエラーが発生しました");
            return;
        }

        // 認証が失敗した場合は処理を停止
        if (!authenticationSuccessful) {
            logger.warn("[{}] 認証失敗により処理を停止: {} {}", requestId, method, requestURI);
            handleUnauthorizedAccess(response, requestId, "認証が必要です");
            return;
        }

        logger.debug("[{}] 認証成功: {} {}", requestId, method, requestURI);
        filterChain.doFilter(request, response);
    }

    /**
     * パブリックエンドポイントかどうかを判定
     */
    private boolean isPublicEndpoint(String requestURI, String method) {
        List<String> fullyPublicEndpoints = securityEndpointsConfig.getFullyPublicEndpoints();
        List<String> publicGetEndpoints = securityEndpointsConfig.getPublicGetEndpoints();

        // 完全にパブリックなエンドポイント
        boolean isFullyPublic = fullyPublicEndpoints.stream()
            .anyMatch(endpoint -> pathMatcher.match(endpoint, requestURI));

        // GETメソッドのみパブリックなエンドポイント
        boolean isPublicGet = "GET".equals(method) && publicGetEndpoints.stream()
            .anyMatch(endpoint -> pathMatcher.match(endpoint, requestURI));

        return isFullyPublic || isPublicGet;
    }

    /**
     * 認証処理を実行
     * 
     * @return 認証が成功した場合はtrue、失敗した場合はfalse
     */
    private boolean processAuthentication(HttpServletRequest request, String requestId) {
        String token = extractTokenFromHeader(request);

        // トークンが存在しない場合は認証失敗
        if (!StringUtils.hasText(token)) {
            logger.debug("[{}] Authorization headerにトークンが存在しません", requestId);
            return false;
        }

        // トークンの検証
        if (!jwtUtil.validateToken(token)) {
            logger.warn("[{}] 無効なトークンです: {}", requestId, maskToken(token));
            return false;
        }

        // ユーザー情報の取得と認証コンテキストの設定
        try {
            return setAuthenticationContext(token, request, requestId);
        } catch (UsernameNotFoundException e) {
            logger.warn("[{}] ユーザーが見つかりません: {}", requestId, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("[{}] 認証コンテキスト設定エラー: {}", requestId, e.getMessage());
            return false;
        }
    }

    /**
     * 認証コンテキストを設定
     */
    private boolean setAuthenticationContext(
        String token,
        HttpServletRequest request,
        String requestId
    ) {
        String email = jwtUtil.getSubjectFromToken(token);
        if (!StringUtils.hasText(email)) {
            logger.warn("[{}] トークンからメールアドレスを取得できません", requestId);
            return false;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (userDetails == null) {
            logger.warn("[{}] ユーザー詳細情報を取得できません: {}", requestId, email);
            return false;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.debug("[{}] 認証コンテキストを設定しました: {}", requestId, email);

        return true;
    }

    /**
     * Authorizationヘッダーからトークンを抽出
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * トークンをマスクして安全にログ出力
     */
    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "null";
        }
        if (token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 5) + "***" + token.substring(token.length() - 5);
    }

    /**
     * 認証エラー時のレスポンス処理
     */
    private void handleAuthenticationError(
        HttpServletResponse response,
        String requestId,
        String message
    ) {
        try {
            response.setContentType(CONTENT_TYPE_JSON);
            response.setCharacterEncoding(CHARSET_UTF8);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            String errorJson = String.format(
                "{\"error\": \"%s\", \"requestId\": \"%s\", \"timestamp\": \"%s\"}",
                message,
                requestId,
                System.currentTimeMillis()
            );

            response.getWriter().write(errorJson);
            response.getWriter().flush();
        } catch (IOException e) {
            logger.error("[{}] エラーレスポンスの書き込みに失敗: {}", requestId, e.getMessage());
        }
    }

    /**
     * 未認証アクセス時のレスポンス処理
     */
    private void handleUnauthorizedAccess(
        HttpServletResponse response,
        String requestId,
        String message
    ) {
        try {
            response.setContentType(CONTENT_TYPE_JSON);
            response.setCharacterEncoding(CHARSET_UTF8);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            String errorJson = String.format(
                "{\"error\": \"%s\", \"requestId\": \"%s\", \"timestamp\": \"%s\"}",
                message,
                requestId,
                System.currentTimeMillis()
            );

            response.getWriter().write(errorJson);
            response.getWriter().flush();
        } catch (IOException e) {
            logger.error("[{}] 未認証レスポンスの書き込みに失敗: {}", requestId, e.getMessage());
        }
    }
}
