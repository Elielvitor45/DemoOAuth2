package next.OAuth.demoOAuth.security;

import next.OAuth.demoOAuth.service.CustomOAuth2UserService;
import next.OAuth.demoOAuth.service.CustomOidcUserService;
import next.OAuth.demoOAuth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;
    
    @Autowired
    private CustomOidcUserService customOidcUserService;
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/tiktok/**"))  // ✅ LIBERA CSRF PARA /tiktok/**
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/login**"),
                                new AntPathRequestMatcher("/register**"),
                                new AntPathRequestMatcher("/api/register"),
                                new AntPathRequestMatcher("/error"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/oauth2/**"),
                                new AntPathRequestMatcher("/oauth/tiktok"),
                                new AntPathRequestMatcher("/auth/tiktok/callback/"),
                                new AntPathRequestMatcher("/tiktok/**")  // ✅ LIBERA /tiktok/**
                        ).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform-login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .failureForwardUrl("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                        .defaultSuccessUrl("/home", true))
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("frame-ancestors 'self' https://rheumatoidally-ventriloquistic-debi.ngrok-free.dev")
                        )
                );
        
        return http.build();
    }
}
