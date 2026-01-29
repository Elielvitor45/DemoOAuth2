package next.OAuth.demoOAuth.service;

import next.OAuth.demoOAuth.model.User;
import next.OAuth.demoOAuth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("===== Tentando fazer login com email: " + email + " =====");
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        
        System.out.println("===== Usuário encontrado! Nome: " + user.getName() + " =====");
        System.out.println("===== Tem senha: " + (user.getPassword() != null && !user.getPassword().isEmpty()) + " =====");
        
        // Verificar se o usuário tem senha cadastrada
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            System.out.println("===== ERRO: Este usuário não possui senha (cadastrado via OAuth) =====");
            throw new UsernameNotFoundException(
                "Este email foi cadastrado via rede social (Google/Facebook/GitHub). " +
                "Por favor, faça login usando a rede social."
            );
        }
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}
