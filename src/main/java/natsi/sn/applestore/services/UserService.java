package natsi.sn.applestore.services;

import natsi.sn.applestore.data.models.User;
import natsi.sn.applestore.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.Optional;

public interface UserService {

    public Optional<User> findByEmail(String email);
    public boolean existsByEmail(String email) ;
    public User registerUser(User user);
    public User save(User user);
    public void updateLastLogin(Long userId);
    public long countUsers();
}




