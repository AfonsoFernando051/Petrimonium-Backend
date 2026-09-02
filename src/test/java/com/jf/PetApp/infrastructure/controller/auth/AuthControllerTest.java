package com.jf.PetApp.infrastructure.controller.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jf.PetApp.application.auth.dto.GoogleLoginCommand;
import com.jf.PetApp.application.auth.dto.LoginCommand;
import com.jf.PetApp.application.auth.dto.LoginResult;
import com.jf.PetApp.application.auth.dto.LogoutCommand;
import com.jf.PetApp.application.auth.dto.RefreshTokenCommand;
import com.jf.PetApp.application.auth.dto.RefreshTokenResult;
import com.jf.PetApp.application.auth.dto.RegisterCommand;
import com.jf.PetApp.application.auth.dto.RegisterResult;
import com.jf.PetApp.application.auth.exception.AuthenticationException;
import com.jf.PetApp.application.auth.exception.PasswordResetTokenInvalidException;
import com.jf.PetApp.application.auth.exception.UserAlreadyExistsException;
import com.jf.PetApp.application.auth.usecase.GoogleLoginUseCase;
import com.jf.PetApp.application.auth.usecase.LoginUseCase;
import com.jf.PetApp.application.auth.usecase.LogoutUseCase;
import com.jf.PetApp.application.auth.usecase.RefreshTokenUseCase;
import com.jf.PetApp.application.auth.usecase.RegisterUserUseCase;
import com.jf.PetApp.application.auth.usecase.RequestPasswordResetUseCase;
import com.jf.PetApp.application.auth.usecase.ResetPasswordUseCase;
import com.jf.PetApp.infrastructure.security.jwt.JwtAuthenticationFilter;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to test only web layer
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private GoogleLoginUseCase googleLoginUseCase;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @MockitoBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockitoBean
    private LogoutUseCase logoutUseCase;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter; // mock the exact filter that security config uses

    // ── /auth/login ──────────────────────────────────────────────────────

    @Test
    void login_WithValidCredentials_ReturnsAccessToken() throws Exception {
        when(loginUseCase.execute(new LoginCommand("investor@test.com", "Str0ngPass")))
                .thenReturn(new LoginResult("a.jwt.token", "a.refresh.token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"investor@test.com","password":"Str0ngPass"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("a.refresh.token"));
    }

    @Test
    void login_WithInvalidCredentials_Returns401() throws Exception {
        when(loginUseCase.execute(new LoginCommand("investor@test.com", "wrong")))
                .thenThrow(new AuthenticationException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"investor@test.com","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_WithBlankEmail_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":"Str0ngPass"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── /auth/google ─────────────────────────────────────────────────────

    @Test
    void google_WithValidIdToken_ReturnsAccessToken() throws Exception {
        when(googleLoginUseCase.execute(new GoogleLoginCommand("valid-id-token")))
                .thenReturn(new LoginResult("a.jwt.token", "a.refresh.token"));

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"valid-id-token"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("a.jwt.token"));
    }

    @Test
    void google_WithInvalidIdToken_Returns401() throws Exception {
        when(googleLoginUseCase.execute(new GoogleLoginCommand("bad-token")))
                .thenThrow(new AuthenticationException("Invalid Google token"));

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":"bad-token"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void google_WithBlankIdToken_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── /auth/register ───────────────────────────────────────────────────

    @Test
    void register_WithValidData_Returns201WithUserIdAndEmail() throws Exception {
        when(registerUserUseCase.execute(new RegisterCommand("investor", "investor@test.com", "Str0ngPass1")))
                .thenReturn(new RegisterResult(42L, "investor", "investor@test.com"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"investor","email":"investor@test.com","password":"Str0ngPass1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.username").value("investor"))
                .andExpect(jsonPath("$.email").value("investor@test.com"));
    }

    @Test
    void register_WithAlreadyRegisteredEmail_Returns409() throws Exception {
        when(registerUserUseCase.execute(new RegisterCommand("investor", "investor@test.com", "Str0ngPass1")))
                .thenThrow(new UserAlreadyExistsException());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"investor","email":"investor@test.com","password":"Str0ngPass1"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
    }

    @Test
    void register_WithWeakPassword_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"investor","email":"investor@test.com","password":"weak"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── /auth/forgot-password ────────────────────────────────────────────

    @Test
    void forgotPassword_WithExistingEmail_ReturnsGenericSuccessMessage() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"investor@test.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(requestPasswordResetUseCase).execute("investor@test.com");
    }

    @Test
    void forgotPassword_WithUnknownEmail_StillReturns200WithSameGenericMessage() throws Exception {
        // RequestPasswordResetUseCase silently no-ops for an unknown email; the controller
        // must not treat that differently — this is the enumeration-avoidance guarantee.
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@test.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void forgotPassword_WithInvalidEmailFormat_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── /auth/reset-password ─────────────────────────────────────────────

    @Test
    void resetPassword_WithValidToken_Returns200() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"raw-token-value","newPassword":"NewStr0ngPass"}"""))
                .andExpect(status().isOk());

        verify(resetPasswordUseCase).execute("raw-token-value", "NewStr0ngPass");
    }

    @Test
    void resetPassword_WithInvalidOrExpiredToken_Returns400() throws Exception {
        doThrow(new PasswordResetTokenInvalidException())
                .when(resetPasswordUseCase).execute(eq("bad-token"), eq("NewStr0ngPass"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"bad-token","newPassword":"NewStr0ngPass"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void resetPassword_WithWeakNewPassword_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"raw-token-value","newPassword":"weak"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── /auth/refresh ─────────────────────────────────────────────────────

    @Test
    void refresh_WithValidRefreshToken_ReturnsNewTokenPair() throws Exception {
        when(refreshTokenUseCase.execute(new RefreshTokenCommand("old-refresh-token")))
                .thenReturn(new RefreshTokenResult("new.jwt.token", "new-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"old-refresh-token"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_WithInvalidRefreshToken_Returns401() throws Exception {
        when(refreshTokenUseCase.execute(new RefreshTokenCommand("bad-token")))
                .thenThrow(new AuthenticationException("Refresh token is invalid, expired, or revoked"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"bad-token"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void refresh_WithBlankRefreshToken_Returns400ValidationError() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ── /auth/logout ──────────────────────────────────────────────────────

    @Test
    void logout_WithRefreshToken_Returns200AndRevokesIt() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"a-refresh-token"}"""))
                .andExpect(status().isOk());

        verify(logoutUseCase).execute(new LogoutCommand("a-refresh-token"));
    }

    @Test
    void logout_WithAlreadyInvalidToken_StillReturns200() throws Exception {
        // Logout is idempotent by design (LogoutUseCaseImpl) — an unknown/already-revoked
        // token still looks like success to the client, since the end state (not
        // authenticated) is the same either way.
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"already-revoked-or-unknown"}"""))
                .andExpect(status().isOk());
    }
}
