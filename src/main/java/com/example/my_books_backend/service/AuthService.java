package com.example.my_books_backend.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.my_books_backend.dto.auth.LoginRequest;
import com.example.my_books_backend.dto.auth.SignupRequest;
import com.example.my_books_backend.dto.auth.AccessTokenResponse;
import com.example.my_books_backend.dto.user.CreateUserRequest;
import com.example.my_books_backend.entity.User;
import com.example.my_books_backend.exception.ConflictException;
import com.example.my_books_backend.exception.UnauthorizedException;
import com.example.my_books_backend.exception.ValidationException;
import com.example.my_books_backend.repository.UserRepository;
import com.example.my_books_backend.service.impl.UserDetailsServiceImpl;
import com.example.my_books_backend.util.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtil;

    /**
     * ユーザーのログイン処理 メールアドレスとパスワードを検証し、
     * 認証成功時にアクセストークンとリフレッシュトークンを発行する。
     * リフレッシュトークンはCookieとして設定。
     * 
     * @param request ログインリクエスト（メールアドレスとパスワードを含む）
     * @param response HTTPレスポンス（Cookieの設定に使用）
     * @return アクセストークンを含むレスポンス
     */
    @Transactional(readOnly = true)
    public AccessTokenResponse login(LoginRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
        } catch (AuthenticationException e) {
            throw new UnauthorizedException("ログインに失敗しました。メールアドレスまたはパスワードが無効です。");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        Cookie refreshTokenCookie = jwtUtil.createRefreshTokenCookie(refreshToken);
        response.addCookie(refreshTokenCookie);

        return new AccessTokenResponse(accessToken);
    }

    /**
     * 新規ユーザーのサインアップ処理 ユーザー情報を検証し、
     * 新規ユーザーを作成した後、アクセストークンとリフレッシュトークンを発行する。
     * リフレッシュトークンはCookieとして設定。
     * 
     * @param request サインアップリクエスト（メールアドレス、パスワード、名前、アバターパスを含む）
     * @param response HTTPレスポンス（Cookieの設定に使用）
     * @return アクセストークンを含むレスポンス
     */
    @Transactional
    public AccessTokenResponse signup(SignupRequest request, HttpServletResponse response) {
        String email = request.getEmail();
        String password = request.getPassword();
        String name = request.getName();
        String avatarPath = request.getAvatarPath();

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("サインアップに失敗しました。このメールアドレスは既に登録されています。: " + email);
        }

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setEmail(email);
        createUserRequest.setPassword(password);
        createUserRequest.setName(name);
        createUserRequest.setAvatarPath(avatarPath);

        User user = userService.createUser(createUserRequest);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        Cookie refreshTokenCookie = jwtUtil.createRefreshTokenCookie(refreshToken);
        response.addCookie(refreshTokenCookie);

        return new AccessTokenResponse(accessToken);
    }

    /**
     * ユーザーのログアウト処理 リフレッシュトークンを無効化するためのCookieを設定。
     * 
     * @param response HTTPレスポンス（Cookieの設定に使用）
     */
    public void logout(HttpServletResponse response) {
        Cookie cookie = jwtUtil.getInvalidateRefreshTokenCookie();
        response.addCookie(cookie);
    }

    /**
     * リフレッシュトークンを使用して新しいアクセストークンを発行。
     * リフレッシュトークンを検証し、有効な場合は新しいアクセストークンを返す。
     * 同時に認証コンテキストも更新。
     * 
     * @param request HTTPリクエスト（リフレッシュトークンの取得に使用）
     * @return 新しいアクセストークンを含むレスポンス
     */
    @Transactional(readOnly = true)
    public AccessTokenResponse refreshAccessToken(HttpServletRequest request) {
        String refreshToken = jwtUtil.getRefreshTokenFromCookie(request);

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            throw new ValidationException("リフレッシュトークンが無効です。");
        }

        String email = jwtUtil.getSubjectFromToken(refreshToken);
        User user = (User) userDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            user,
            null,
            user.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtil.generateAccessToken(user);
        return new AccessTokenResponse(accessToken);
    }
}
