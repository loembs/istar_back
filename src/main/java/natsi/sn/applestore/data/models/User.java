package natsi.sn.applestore.data.models;
import jakarta.persistence.*;
import lombok.Data;
import natsi.sn.applestore.data.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
@Entity
@Table(name = "users")
@Data
public class User implements UserDetails{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable=false)
    private String nomcomplet;

    @Enumerated(EnumType.STRING)
    private Role role = Role.CLIENT;
    private String phone ;
    private String address;
    private Boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLogin;

    //OAuth fields
    private String oauthProvider; // GOOGLE, FACEBOOK, etc.
    private String oauthId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
        return enabled;
    }

    public String getFullName() {
        return nomcomplet;
    }
}
