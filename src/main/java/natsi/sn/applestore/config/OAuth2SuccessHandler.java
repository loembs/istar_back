package natsi.sn.applestore.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.repository.UserRepository;
import natsi.sn.applestore.security.JwtUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.security.oauth2.client.registration.google",
        name = "client-id",
        havingValue = "",
        matchIfMissing = false
)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String provider = "GOOGLE";
        String oauthId = (String) attributes.get("sub");

        // Chercher ou créer l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setNomcomplet(name != null ? name : email);
                    newUser.setOauthProvider(provider);
                    newUser.setOauthId(oauthId);
                    newUser.setRole(natsi.sn.applestore.data.enums.Role.CLIENT);
                    newUser.setEnabled(true);
                    return userRepository.save(newUser);
                });

        // Générer le token JWT
        String token = jwtUtils.generateToken(user, user.getRole().name());

        // Rediriger vers le frontend avec le token
        String redirectUrl = String.format("%s/auth/oauth2/callback?token=%s",
                getDefaultTargetUrl(), token);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

