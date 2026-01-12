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

import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RestTemplate restTemplate;

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
    public void googleOAuth(
            @RequestParam(required = false) String returnUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String clientId = System.getenv("GOOGLE_CLIENT_ID");
        if (clientId == null || clientId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Google OAuth not configured");
            return;
        }

        // Construire l'URL de redirection
        String redirectUri = System.getenv("GOOGLE_REDIRECT_URI");
        if (redirectUri == null || redirectUri.isEmpty()) {
            redirectUri = "https://istar-back.onrender.com/api/auth/oauth2/callback";
        }

        // Stocker returnUrl dans la session
        if (returnUrl != null && !returnUrl.isEmpty()) {
            HttpSession session = request.getSession();
            session.setAttribute("oauth_return_url", returnUrl);
        }

        // Construire l'URL d'autorisation Google
        String googleAuthUrl = String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=%s&" +
                        "redirect_uri=%s&" +
                        "response_type=code&" +
                        "scope=openid%%20profile%%20email&" +
                        "access_type=offline&" +
                        "prompt=consent",
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
        );

        response.sendRedirect(googleAuthUrl);
    }
    @GetMapping("/oauth2/callback")
    public void oauth2Callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String frontendUrl = System.getenv("FRONTEND_URL");
        if (frontendUrl == null || frontendUrl.isEmpty()) {
            frontendUrl = "https://apple-store-hazel.vercel.app";
        }

        // Gérer les erreurs
        if (error != null) {
            response.sendRedirect(frontendUrl + "/login?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8));
            return;
        }

        if (code == null || code.isEmpty()) {
            response.sendRedirect(frontendUrl + "/login?error=" + URLEncoder.encode("Code d'autorisation manquant", StandardCharsets.UTF_8));
            return;
        }

        try {
            String clientId = System.getenv("GOOGLE_CLIENT_ID");
            String clientSecret = System.getenv("GOOGLE_CLIENT_SECRET");
            String redirectUri = System.getenv("GOOGLE_REDIRECT_URI");
            if (redirectUri == null || redirectUri.isEmpty()) {
                redirectUri = "https://istar-back.onrender.com/api/auth/oauth2/callback";
            }

            if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
                response.sendRedirect(frontendUrl + "/login?error=" + URLEncoder.encode("Google OAuth non configuré", StandardCharsets.UTF_8));
                return;
            }

            // 1. Échanger le code contre un token d'accès
            String tokenUrl = "https://oauth2.googleapis.com/token";
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            Map<String, String> tokenParams = new HashMap<>();
            tokenParams.put("code", code);
            tokenParams.put("client_id", clientId);
            tokenParams.put("client_secret", clientSecret);
            tokenParams.put("redirect_uri", redirectUri);
            tokenParams.put("grant_type", "authorization_code");

            StringBuilder tokenBody = new StringBuilder();
            for (Map.Entry<String, String> entry : tokenParams.entrySet()) {
                if (tokenBody.length() > 0) {
                    tokenBody.append("&");
                }
                tokenBody.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }

            HttpEntity<String> tokenRequest = new HttpEntity<>(tokenBody.toString(), tokenHeaders);
            ResponseEntity<String> tokenResponse = restTemplate.exchange(tokenUrl, HttpMethod.POST, tokenRequest, String.class);

            if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
                response.sendRedirect(frontendUrl + "/login?error=" + URLEncoder.encode("Erreur lors de l'échange du code", StandardCharsets.UTF_8));
                return;
            }

            // 2. Extraire le token d'accès
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
            String accessToken = tokenJson.get("access_token").asText();

            // 3. Récupérer les informations de l'utilisateur Google
            String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.setBearerAuth(accessToken);
            HttpEntity<String> userInfoRequest = new HttpEntity<>(userInfoHeaders);
            ResponseEntity<String> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userInfoRequest, String.class);

            if (!userInfoResponse.getStatusCode().is2xxSuccessful() || userInfoResponse.getBody() == null) {
                response.sendRedirect(frontendUrl + "/login?error=" + URLEncoder.encode("Erreur lors de la récupération des informations utilisateur", StandardCharsets.UTF_8));
                return;
            }

            // 4. Parser les informations utilisateur
            JsonNode userInfo = objectMapper.readTree(userInfoResponse.getBody());
            String googleId = userInfo.get("id").asText();
            String email = userInfo.get("email").asText();
            String name = userInfo.has("name") ? userInfo.get("name").asText() : email;
            String picture = userInfo.has("picture") ? userInfo.get("picture").asText() : null;

            // 5. Créer ou mettre à jour l'utilisateur
            User user = userService.findByEmail(email).orElse(null);
            if (user == null) {
                // Créer un nouvel utilisateur
                user = new User();
                user.setEmail(email);
                user.setNomcomplet(name);
                user.setOauthProvider("GOOGLE");
                user.setOauthId(googleId);
                user.setRole(Role.CLIENT);
                user.setEnabled(true);
                user.setPassword(passwordEncoder.encode("OAUTH_USER_" + System.currentTimeMillis())); // Mot de passe aléatoire
                user = userService.registerUser(user);
            } else {
                // Mettre à jour l'utilisateur existant
                if (user.getOauthProvider() == null || !user.getOauthProvider().equals("GOOGLE")) {
                    user.setOauthProvider("GOOGLE");
                    user.setOauthId(googleId);
                }
                userService.updateLastLogin(user.getId());
                user = userService.save(user);
            }

            // 6. Générer un JWT token
            String token = jwtUtils.generateToken(user, user.getRole().name());

            // 7. Récupérer le returnUrl de la session
            HttpSession session = request.getSession();
            String returnUrl = (String) session.getAttribute("oauth_return_url");
            if (returnUrl == null || returnUrl.isEmpty()) {
                returnUrl = "/";
            }
            session.removeAttribute("oauth_return_url");

            // 8. Rediriger vers le frontend avec le token
            String redirectUrl = frontendUrl + "/auth/oauth2/callback?token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8) +
                    "&returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect(frontendUrl + "/login?error=" + URLEncoder.encode("Erreur lors de la connexion: " + e.getMessage(), StandardCharsets.UTF_8));
        }
    }
}


