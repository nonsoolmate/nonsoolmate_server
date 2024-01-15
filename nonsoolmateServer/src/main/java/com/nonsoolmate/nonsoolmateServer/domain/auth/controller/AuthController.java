package com.nonsoolmate.nonsoolmateServer.domain.auth.controller;

import com.nonsoolmate.nonsoolmateServer.domain.auth.controller.dto.request.MemberRequestDTO;
import com.nonsoolmate.nonsoolmateServer.domain.auth.controller.dto.response.MemberAuthResponseDTO;
import com.nonsoolmate.nonsoolmateServer.domain.auth.controller.dto.response.MemberReissueResponseDTO;
import com.nonsoolmate.nonsoolmateServer.external.oauth.service.vo.enums.AuthType;
import com.nonsoolmate.nonsoolmateServer.global.jwt.service.JwtService;
import com.nonsoolmate.nonsoolmateServer.domain.auth.service.AuthServiceProvider;
import com.nonsoolmate.nonsoolmateServer.domain.auth.service.vo.MemberSignUpVO;
import com.nonsoolmate.nonsoolmateServer.global.response.ApiResponse;
import com.nonsoolmate.nonsoolmateServer.domain.auth.exception.AuthSuccessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Slf4j
@Tag(name = "auth", description = "인증 관련 API")
public class AuthController {
    private final AuthServiceProvider authServiceProvider;
    private final JwtService jwtService;

    @Value("${spring.security.oauth2.client.naver.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.naver.redirect-uri}")
    private String redirectUri;

    @Operation(summary = "소셜 로그인", description = "네이버 소셜 로그인을 합니다.")
    @PostMapping("/social/login")
    public ResponseEntity<ApiResponse<MemberAuthResponseDTO>> login(
            @RequestHeader(value = "authorization-code") final String authorizationCode,
            @RequestBody @Valid final
            MemberRequestDTO request, HttpServletResponse response) {
        MemberSignUpVO vo = authServiceProvider.getAuthService(request.platformType())
                .saveMemberOrLogin(authorizationCode, request);
        MemberAuthResponseDTO responseDTO = jwtService.issueToken(vo, response);
        if (responseDTO.authType().equals(AuthType.SIGN_UP)) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(AuthSuccessType.SIGN_UP_SUCCESS, responseDTO));
        }
        return ResponseEntity.ok().body(ApiResponse.success(AuthSuccessType.LOGIN_SUCCESS, responseDTO));
    }

    @Operation(summary = "액세스 토큰 & 리프레시 토큰 재발급", description = "액세스 토큰 및 리프레시 토큰을 재발급 받습니다.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<MemberReissueResponseDTO>> reissue(HttpServletRequest request,
                                                                         HttpServletResponse response) {
        MemberReissueResponseDTO memberReissueResponseDTO = jwtService.reissueToken(request, response);
        return ResponseEntity.ok().body(ApiResponse.success(AuthSuccessType.REISSUE_SUCCESS, memberReissueResponseDTO));
    }

    @GetMapping("/authTest")
    public String authTest(HttpServletRequest request, HttpServletResponse response) {
        String redirectURL = "https://nid.naver.com/oauth2.0/authorize?client_id=" + clientId
                + "&redirect_uri=" + redirectUri + "&response_type=code";
        try {
            response.sendRedirect(
                    redirectURL);
        } catch (Exception e) {
            log.info("authTest = {}", e);
        }

        return "SUCCESS";
    }
}
