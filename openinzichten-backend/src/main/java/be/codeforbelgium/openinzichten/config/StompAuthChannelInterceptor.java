package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final JwtService jwtService;

    @Autowired
    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = null;
            // Expect the token in the Authorization header as: "Bearer <token>"
            List<String> auth = accessor.getNativeHeader("Authorization");
            if (auth == null || auth.isEmpty())
                auth = accessor.getNativeHeader("authorization");
            if (auth == null || auth.isEmpty())
                auth = accessor.getNativeHeader("AUTHORIZATION");
            if (auth != null && !auth.isEmpty()) {
                String header = auth.get(0);
                if (header != null && header.startsWith("Bearer ")) {
                    token = header.substring(7);
                }
            }

            if (token != null && jwtService.isTokenValid(token)) {
                try {
                    String username = jwtService.extractUsername(token);
                    List<String> roles = jwtService.extractRoles(token);
                    List<GrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .map(a -> (GrantedAuthority) a)
                            .toList();

                    var userDetails = User.withUsername(username).password("").authorities(authorities).build();
                    var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    accessor.setUser(authToken);
                    log.debug("User {} authenticated successfully", username);
                } catch (Exception e) {
                    log.debug("Failed to set WebSocket principal: {}", e.getMessage());
                }
            } else {
                log.debug("No valid Authorization Bearer token found on STOMP CONNECT");
            }
        }

        return message;
    }
}
