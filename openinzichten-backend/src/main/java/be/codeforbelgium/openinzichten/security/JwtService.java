package be.codeforbelgium.openinzichten.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final Key key;
    private long expirationMs = 1000L * 60 * 60 * 24; // 24h

    public JwtService(@Value("${app.jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            // generate ephemeral key if not provided (development only)
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            // expect base64-encoded secret
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(UUID id, String username, String email, List<String> roles, boolean rememberMe) {
        Date now = new Date();
        if (rememberMe) {
            expirationMs = 1000L * 60 * 60 * 24 * 90;
        }
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("id", id.toString())
                .claim("role", roles)
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public UUID extractId(String token) {
        String idStr = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .get("id", String.class);
        if (idStr == null)
            return null;
        return UUID.fromString(idStr);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object rolesObj = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token)
                .getBody()
                .get("role");
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
