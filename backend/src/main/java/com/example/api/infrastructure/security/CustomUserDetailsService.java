package com.example.api.infrastructure.security;

import com.example.api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var domainUser = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return User.withUsername(domainUser.getEmail() != null ? domainUser.getEmail() : String.valueOf(domainUser.getId()))
            .password(domainUser.getPassword() != null ? domainUser.getPassword() : "")
            .roles("USER")
            .build();
    }
}
