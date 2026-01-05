package com.supabase.auth.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Détails de l'utilisateur pour Spring Security
 */
@Data
public class SupabaseUserDetails implements UserDetails {

    private final String supabaseId;
    private final String email;
    private final Long localUserId;
    private final String fullName;
    private final List<GrantedAuthority> authorities;

    public SupabaseUserDetails(String supabaseId, String email, Long localUserId, String fullName, List<String> roles) {
        this.supabaseId = supabaseId;
        this.email = email;
        this.localUserId = localUserId;
        this.fullName = fullName;
        this.authorities = roles != null 
            ? roles.stream()
                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .collect(java.util.stream.Collectors.toList())
            : Collections.emptyList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Pas de mot de passe pour les utilisateurs OAuth
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
