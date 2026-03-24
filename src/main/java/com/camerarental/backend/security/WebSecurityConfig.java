package com.camerarental.backend.security;





import com.camerarental.backend.security.filters.LoginRateLimitingFilter;
import com.camerarental.backend.security.filters.RequestLoggingFilter;
import com.camerarental.backend.security.jwt.AuthEntryPointJwt;
import com.camerarental.backend.security.jwt.AuthTokenFilter;
import com.camerarental.backend.security.services.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final LoginRateLimitingFilter loginRateLimitingFilter;

    @Value("${security.auth.allowed-origins}")
    private List<String> allowedOrigins;  // Spring Boot will split on commas

    @Value("${app.security.allow-docs:false}")
    private boolean allowDocs;

    @Value("${spring.h2.console.enabled:false}")
    private boolean allowH2;

    private static final String[] DOC_ENDPOINTS = {
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    private static final String[] H2_ENDPOINTS = { "/h2-console/**" };

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager (AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * This bean determines which origins are allowed for this backend.
     * @return OriginCheckFilter with allowedOrigins and
     */
    @Bean
    public OriginCheckFilter originCheckFilter() {
        // TODO: later read origins from application.properties
        return new OriginCheckFilter(
                allowedOrigins,
                List.of(
                        "/api/v1/auth/signin",
                        "/api/v1/auth/signup"
                )
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // reuse your property: security.auth.allowed-origins=
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }


    /**
     * This config disables CSRF entirely and relies on JWT + OriginCheck for protection.
     * @param http
     * @param authTokenFilter
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthTokenFilter authTokenFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable) // this is  .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint((unauthorizedHandler)))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        // This disables the default CompositeSessionAuthenticationStrategy
                        // (which includes CsrfAuthenticationStrategy that clears the token)
                        .sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy())
                )
                .authorizeHttpRequests((authorizeRequests) -> {
                        authorizeRequests
                                .requestMatchers("/api/v1/auth/signin", "/api/v1/auth/signup").permitAll()
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/api/test/**").permitAll()
                                .requestMatchers("/images/**").permitAll()
                                .requestMatchers("/actuator/health/**").permitAll()
                                .requestMatchers("/api/v1/business-hours/**").permitAll();

                                if (allowDocs) {
                                    authorizeRequests.requestMatchers(DOC_ENDPOINTS).permitAll();
                                }
                                if (allowH2) {
                                    authorizeRequests.requestMatchers(H2_ENDPOINTS).permitAll();
                                }
                                authorizeRequests.anyRequest().authenticated();
             });

        http.addFilterBefore(loginRateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(new RequestLoggingFilter(), UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(originCheckFilter(), UsernamePasswordAuthenticationFilter.class);
        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authTokenFilter,  // adding custom filter before UsernamePasswordAuthenticationFilter
                UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers.frameOptions(
                HeadersConfigurer.FrameOptionsConfig::sameOrigin
        ));

        return http.build();
    }

    // Used to define which endpoints should bypass spring security entirely at a global level
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web -> web.ignoring().requestMatchers(
                "/v2/api-docs",
                "/configuration/ui",
                "/swagger-resources/**",
                "/swagger-ui.html",
                "/webjars/**"
        ));
    }
}
