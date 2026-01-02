package natsi.sn.applestore.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.enums.Role;
import natsi.sn.applestore.security.JwtUtils;
import natsi.sn.applestore.services.UserService;
import natsi.sn.applestore.services.OtpService;
import natsi.sn.applestore.web.dto.request.LoginRequest;
import natsi.sn.applestore.web.dto.request.SignupRequest;
import natsi.sn.applestore.web.dto.request.SendOtpRequest;
import natsi.sn.applestore.web.dto.request.VerifyOtpRequest;
import natsi.sn.applestore.web.dto.response.AuthResponse;
import natsi.sn.applestore.web.dto.response.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Email requis")
                                .build());
            }

            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Mot de passe requis")
                                .build());
            }

            if (!userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Email ou mot de passe incorrect")
                                .build());
            }

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

            userService.updateLastLogin(user.getId());

            String token = jwtUtils.generateToken(user, user.getRole().name());
            String refreshToken = jwtUtils.generateToken(user);

            UserResponse userResponse = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nomcomplet(user.getNomcomplet())
                    .role(user.getRole())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .enabled(user.getEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .oauthProvider(user.getOauthProvider())
                    .oauthId(user.getOauthId())
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userResponse)
                    .message("Connexion réussie")
                    .build();

            return ResponseEntity.ok(response);
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Email ou mot de passe incorrect")
                            .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Erreur lors de la connexion: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignupRequest request) {
        try {
            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Cet email est déjà utilisé")
                                .build());
            }

            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setNomcomplet(request.getNomcomplet());
            user.setPhone(request.getPhone());
            user.setAddress(request.getAddress());
            user.setRole(Role.CLIENT);
            user.setEnabled(true);

            User savedUser = userService.registerUser(user);

            String token = jwtUtils.generateToken(savedUser, savedUser.getRole().name());
            String refreshToken = jwtUtils.generateToken(savedUser);

            UserResponse userResponse = UserResponse.builder()
                    .id(savedUser.getId())
                    .email(savedUser.getEmail())
                    .nomcomplet(savedUser.getNomcomplet())
                    .role(savedUser.getRole())
                    .phone(savedUser.getPhone())
                    .address(savedUser.getAddress())
                    .enabled(savedUser.getEnabled())
                    .createdAt(savedUser.getCreatedAt())
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userResponse)
                    .message("Inscription réussie")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Erreur lors de l'inscription: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (jwtUtils.validateToken(token, user)) {
                String newToken = jwtUtils.generateToken(user, user.getRole().name());
                String newRefreshToken = jwtUtils.generateToken(user);

                UserResponse userResponse = UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nomcomplet(user.getNomcomplet())
                        .role(user.getRole())
                        .phone(user.getPhone())
                        .address(user.getAddress())
                        .enabled(user.getEnabled())
                        .createdAt(user.getCreatedAt())
                        .lastLogin(user.getLastLogin())
                        .build();

                return ResponseEntity.ok(AuthResponse.builder()
                        .token(newToken)
                        .type("Bearer")
                        .user(userResponse)
                        .message("Token rafraîchi")
                        .build());
            }
        } catch (Exception e) {
            // Token invalide
        }

        return ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                        .message("Token invalide")
                        .build());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtUtils.extractUsername(token);
            User user = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (jwtUtils.validateToken(token, user)) {
                UserResponse userResponse = UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nomcomplet(user.getNomcomplet())
                        .role(user.getRole())
                        .phone(user.getPhone())
                        .address(user.getAddress())
                        .enabled(user.getEnabled())
                        .createdAt(user.getCreatedAt())
                        .lastLogin(user.getLastLogin())
                        .build();

                return ResponseEntity.ok(AuthResponse.builder()
                        .token(token)
                        .type("Bearer")
                        .user(userResponse)
                        .build());
            }
        } catch (Exception e) {
            // Token invalide
        }

        return ResponseEntity.badRequest()
                .body(AuthResponse.builder()
                        .message("Token invalide")
                        .build());
    }

    // ========== 2FA ENDPOINTS ==========

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        try {
            otpService.generateAndSendOtp(request.getEmail(), request.getPurpose());
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Code OTP envoyé à votre email")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Erreur lors de l'envoi du code: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/login-with-otp")
    public ResponseEntity<?> loginWithOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            // Vérifier le code OTP
            if (!otpService.verifyOtp(request.getEmail(), request.getCode(), "LOGIN")) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Code OTP invalide ou expiré")
                                .build());
            }

            // Récupérer l'utilisateur
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            userService.updateLastLogin(user.getId());

            String token = jwtUtils.generateToken(user, user.getRole().name());

            UserResponse userResponse = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nomcomplet(user.getNomcomplet())
                    .role(user.getRole())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .enabled(user.getEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .build();

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userResponse)
                    .message("Connexion réussie avec 2FA")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Erreur lors de la connexion: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/register-with-otp")
    public ResponseEntity<?> registerWithOtp(
            @Valid @RequestBody SignupRequest signupRequest,
            @RequestParam String otpCode) {
        try {
            // Vérifier le code OTP
            if (!otpService.verifyOtp(signupRequest.getEmail(), otpCode, "REGISTER")) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Code OTP invalide ou expiré")
                                .build());
            }

            if (userService.existsByEmail(signupRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.builder()
                                .message("Cet email est déjà utilisé")
                                .build());
            }

            User user = new User();
            user.setEmail(signupRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setNomcomplet(signupRequest.getNomcomplet());
            user.setPhone(signupRequest.getPhone());
            user.setAddress(signupRequest.getAddress());
            user.setRole(Role.CLIENT);
            user.setEnabled(true);

            User savedUser = userService.registerUser(user);

            String token = jwtUtils.generateToken(savedUser, savedUser.getRole().name());

            UserResponse userResponse = UserResponse.builder()
                    .id(savedUser.getId())
                    .email(savedUser.getEmail())
                    .nomcomplet(savedUser.getNomcomplet())
                    .role(savedUser.getRole())
                    .phone(savedUser.getPhone())
                    .address(savedUser.getAddress())
                    .enabled(savedUser.getEnabled())
                    .createdAt(savedUser.getCreatedAt())
                    .build();

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userResponse)
                    .message("Inscription réussie avec 2FA")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Erreur lors de l'inscription: " + e.getMessage())
                            .build());
        }
    }

    // ========== OAUTH GOOGLE ENDPOINTS ==========
    // Note: Le callback OAuth2 est géré par SecurityConfig avec un handler inline
    // Spring Security redirige automatiquement vers /login/oauth2/code/google après l'authentification

    @GetMapping("/oauth2/success")
    public ResponseEntity<?> oauth2Success(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            userService.updateLastLogin(user.getId());

            String token = jwtUtils.generateToken(user, user.getRole().name());

            UserResponse userResponse = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .nomcomplet(user.getNomcomplet())
                    .role(user.getRole())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .enabled(user.getEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastLogin(user.getLastLogin())
                    .oauthProvider(user.getOauthProvider())
                    .oauthId(user.getOauthId())
                    .build();

            return ResponseEntity.ok(AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .user(userResponse)
                    .message("Connexion OAuth réussie")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.builder()
                            .message("Erreur lors de la connexion OAuth: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/oauth2/google")
    public void googleOAuthRedirect(
            @RequestParam(required = false) String returnUrl,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        String redirectUri = System.getenv("GOOGLE_REDIRECT_URI");

        if (clientId == null || redirectUri == null) {
            response.sendError(400, "Google OAuth non configuré");
            return;
        }

        if (returnUrl != null) {
            request.getSession().setAttribute("oauth_return_url", returnUrl);
        }

        String url = String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=%s&redirect_uri=%s&response_type=code" +
                        "&scope=openid%%20email%%20profile",
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        );

        response.sendRedirect(url);
    }
}


