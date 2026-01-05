package com.supabase.auth.security;

import com.supabase.auth.service.SupabaseAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre pour valider les tokens JWT Supabase dans les requêtes HTTP
 */
@Slf4j
@RequiredArgsConstructor
public class SupabaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final SupabaseAuthService supabaseAuthService;
    private final SupabaseUserDetailsService<?> userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // Valider le token Supabase
            String userId = supabaseAuthService.extractUserIdFromToken(jwt);
            String email = supabaseAuthService.extractEmailFromToken(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Charger les détails de l'utilisateur depuis le backend local
                SupabaseUserDetails userDetails = userDetailsService.loadUserBySupabaseId(userId, email);

                if (userDetails != null) {
                    // Créer l'authentification Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.debug("Token Supabase invalide ou erreur: {}", e.getMessage());
            // Continuer sans authentification
        }

        filterChain.doFilter(request, response);
    }
}
