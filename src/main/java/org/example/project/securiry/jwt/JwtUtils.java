package org.example.project.securiry.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.example.project.exception.TokenInValid;
import org.example.project.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;
    private SecretKey secretKey(){
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    public String generateToken(User user , Long expiration){
        Date now = new Date();

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey())
                .compact();
    }
    public boolean validateToken(String token){
        try {
            return Jwts.parser()
                    .verifyWith(secretKey())
                    .build()
                    .parseSignedClaims(token) != null;

        } catch (ExpiredJwtException e){
            throw new TokenInValid("Expired JWT Token");

        } catch (SignatureException e){
            throw new TokenInValid("Invalid JWT Signature");

        } catch (MalformedJwtException e){
            throw new TokenInValid("Invalid JWT Token");

        } catch (Exception e){
            return false;
        }
    }
    public String getUsername(String token){
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public Date getExp(String token){
        return Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload().getExpiration();

    }

}
