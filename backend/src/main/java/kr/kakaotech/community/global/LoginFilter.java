package kr.kakaotech.community.global;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.kakaotech.community.dto.ApiResponse;
import kr.kakaotech.community.dto.request.UserLoginRequest;
import kr.kakaotech.community.dto.request.UserLoginResponse;
import kr.kakaotech.community.exception.CustomException;
import kr.kakaotech.community.exception.ErrorCode;
import kr.kakaotech.community.global.jwt.JwtUtil;
import kr.kakaotech.community.global.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public LoginFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;

        setFilterProcessesUrl("/auth");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            UserLoginRequest userLoginRequest = new ObjectMapper().readValue(request.getInputStream(), UserLoginRequest.class);

            String email = userLoginRequest.getEmail();
            String password = userLoginRequest.getPassword();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            log.info("ERROR: LoginFilter IOException : ",e);
            throw new CustomException(ErrorCode.INVALID_LOGIN_JSON);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            // 기타 에러
            log.error("ERROR: LoginFilter error : ", e);
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        log.info("SUCCESS: LoginFilter successfulAuthentication : {}", authResult);

        CustomUserDetails principal = (CustomUserDetails) authResult.getPrincipal();

        String userId = principal.getUserId();
        String email = principal.getUsername();
        String nickname = principal.getNickname();

        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority().replace("ROLE_", "");

        // 토큰 발행
        String accessToken = jwtUtil.createAccessToken(userId, email, role);
        // TODO: 토큰 db 저장
//        String refreshToken = jwtUtil.createRefreshToken(userId, email, role);

//        refreshTokenService.addRefreshToken(username, refreshToken, jwtUtil.getRefreshExpiration());

        // 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("access", accessToken);
//        response.addCookie(cookieUtil.createCookie("refresh", refreshToken));
        response.setStatus(HttpStatus.OK.value());

        UserLoginResponse userLoginResponse = new UserLoginResponse(accessToken, nickname, userId);
        ApiResponse<UserLoginResponse> apiResponse = new ApiResponse<>("로그인 성공", userLoginResponse);

        objectMapper.writeValue(response.getWriter(), apiResponse);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.info("Fail: LoginFilter unsuccessfulAuthentication Exception : ", failed);
//        throw new CustomException(ErrorCode.LOGIN_FAIL);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        ApiResponse<Void> apiResponse = new ApiResponse<>("로그인 실패: 이메일 또는 비밀번호를 확인해주세요", null);
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
