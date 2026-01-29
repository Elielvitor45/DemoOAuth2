package next.OAuth.demoOAuth.service;

import next.OAuth.demoOAuth.model.User;
import next.OAuth.demoOAuth.model.UserProvider;
import next.OAuth.demoOAuth.repository.UserProviderRepository;
import next.OAuth.demoOAuth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, 
                      UserProviderRepository userProviderRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProviderRepository = userProviderRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public User registerUser(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email já cadastrado!");
        }
        
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setName(name);
        
        User savedUser = userRepository.save(user);
        
        // Criar provider LOCAL
        UserProvider localProvider = new UserProvider(savedUser, "LOCAL", null);
        userProviderRepository.save(localProvider);
        
        System.out.println("===== Usuário LOCAL cadastrado! ID: " + savedUser.getId() + " =====");
        
        return savedUser;
    }
    
    @Transactional
    public User findOrCreateOAuthUser(String email, String name, String provider, String providerId, 
                                     String accessToken, LocalDateTime tokenExpiresAt, String refreshToken, String photoUrl) {
        System.out.println("===== Buscando usuário OAuth =====");
        System.out.println("Email: " + email);
        System.out.println("Provider: " + provider);
        System.out.println("Provider ID: " + providerId);
        System.out.println("Photo URL: " + photoUrl);
        
        // 1. Verificar se já existe um provider com esse provider + providerId
        Optional<UserProvider> existingProvider = userProviderRepository.findByProviderAndProviderId(provider, providerId);
        
        if (existingProvider.isPresent()) {
            UserProvider userProvider = existingProvider.get();
            User user = userProvider.getUser();
            System.out.println("===== Provider já vinculado! User ID: " + user.getId() + " =====");
            
            // Atualizar email/nome/foto se mudou
            boolean userUpdated = false;
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
                userUpdated = true;
            }
            if (!user.getName().equals(name)) {
                user.setName(name);
                userUpdated = true;
            }
            // Atualizar foto se veio uma nova
            if (photoUrl != null && !photoUrl.isEmpty()) {
                user.setPhotoUrl(photoUrl);
                userUpdated = true;
                System.out.println("===== Foto atualizada: " + photoUrl + " =====");
            }
            
            if (userUpdated) {
                userRepository.save(user);
                System.out.println("===== Dados do usuário atualizados =====");
            }
            
            // Atualizar access token e expiração
            userProvider.setAccessToken(accessToken);
            userProvider.setTokenExpiresAt(tokenExpiresAt);
            userProvider.setRefreshToken(refreshToken);
            userProviderRepository.save(userProvider);
            
            System.out.println("===== Access token atualizado! Expira em: " + tokenExpiresAt + " =====");
            
            return user;
        }
        
        // 2. Verificar se existe um usuário com esse email
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            System.out.println("===== Usuário encontrado por email! ID: " + user.getId() + " =====");
            
            // Atualizar foto se não tiver ou veio uma nova
            if (photoUrl != null && !photoUrl.isEmpty()) {
                user.setPhotoUrl(photoUrl);
                userRepository.save(user);
                System.out.println("===== Foto vinculada ao usuário existente: " + photoUrl + " =====");
            }
            
            // Verificar se esse provider já está vinculado
            if (userProviderRepository.existsByUserAndProvider(user, provider)) {
                System.out.println("===== Provider " + provider + " já vinculado a este usuário =====");
                return user;
            }
            
            // Vincular novo provider ao usuário existente
            UserProvider newProvider = new UserProvider(user, provider, providerId);
            newProvider.setAccessToken(accessToken);
            newProvider.setTokenExpiresAt(tokenExpiresAt);
            newProvider.setRefreshToken(refreshToken);
            userProviderRepository.save(newProvider);
            
            System.out.println("===== Novo provider " + provider + " vinculado com token! =====");
            
            return user;
        }
        
        // 3. Criar novo usuário
        System.out.println("===== Criando novo usuário =====");
        
        User newUser = new User(email, name);
        newUser.setPhotoUrl(photoUrl);
        User savedUser = userRepository.save(newUser);
        
        // Criar provider com token
        UserProvider newProvider = new UserProvider(savedUser, provider, providerId);
        newProvider.setAccessToken(accessToken);
        newProvider.setTokenExpiresAt(tokenExpiresAt);
        newProvider.setRefreshToken(refreshToken);
        userProviderRepository.save(newProvider);
        
        System.out.println("===== Novo usuário criado com token e foto! ID: " + savedUser.getId() + " =====");
        
        return savedUser;
    }
    
    // Método de compatibilidade (sem foto)
    @Transactional
    public User findOrCreateOAuthUser(String email, String name, String provider, String providerId, 
                                     String accessToken, LocalDateTime tokenExpiresAt, String refreshToken) {
        return findOrCreateOAuthUser(email, name, provider, providerId, accessToken, tokenExpiresAt, refreshToken, null);
    }
    
    // Método antigo para compatibilidade (sem token)
    @Transactional
    public User findOrCreateOAuthUser(String email, String name, String provider, String providerId) {
        return findOrCreateOAuthUser(email, name, provider, providerId, null, null, null, null);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        return userProviderRepository.findByProviderAndProviderId(provider, providerId)
                .map(UserProvider::getUser);
    }
}
