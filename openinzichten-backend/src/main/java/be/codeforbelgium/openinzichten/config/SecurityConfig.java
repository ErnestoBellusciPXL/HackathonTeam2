package be.codeforbelgium.openinzichten.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, be.codeforbelgium.openinzichten.security.JwtAuthenticationFilter jwtFilter) throws Exception {
        // instantiate a fresh entry point here so that test mocks of the component
        // don't suppress proper 401 responses when the real logic should run
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();

        http
                // for API usage (Postman) we disable CSRF; consider enabling or using tokens in production
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .authorizeHttpRequests(auth -> auth
                        // allow preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // allow Spring Boot error endpoint so exceptions don't appear as 401
                        .requestMatchers("/error").permitAll()
                        // allow actuator health/info and Prometheus scrape endpoint
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        // allow debug endpoints for diagnostics
                        .requestMatchers("/api/debug/**").permitAll()
                        // allow unauthenticated registration
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/check-info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-reset").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-reset/confirm").permitAll()
                        // require authentication for updating zipcode
                        .requestMatchers(HttpMethod.GET, "/api/zipcodes").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/account/**").authenticated()
                        // allow all methods for conditions endpoints
                        .requestMatchers("/api/conditions", "/api/conditions/**").permitAll()
                        // allow public GET access to story read endpoints
                        .requestMatchers(HttpMethod.GET, "/api/stories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stories/byuserid").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/stories/ai/**").authenticated()
                        // allow public GET access to heatmap data
                        .requestMatchers(HttpMethod.GET, "/api/heatmap/**").permitAll()
                        // complete registration requires authentication
                        .requestMatchers(HttpMethod.PUT, "/api/auth/complete").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/connection/invite").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/connection/{userId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/connection/accept").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/connection/reject").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/connection/disconnect").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/connection/cancel").authenticated()
                        // chat endpoints require authentication
                        .requestMatchers("/api/chat/**").authenticated()
                        // allow websocket handshake endpoints (SockJS) — authentication is handled on STOMP CONNECT
                        .requestMatchers("/api/ws/**").permitAll()
                        // use JWT Bearer tokens instead of http basic
                        .requestMatchers("/api/admin/**").hasAuthority("Admin")
                        // allow actuator/health or static resources if needed (add more matchers here)
                        .anyRequest().authenticated()
                )
                // use JWT Bearer tokens instead of http basic
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
