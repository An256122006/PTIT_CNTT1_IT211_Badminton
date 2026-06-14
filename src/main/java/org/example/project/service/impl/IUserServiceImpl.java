package org.example.project.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.project.common.reponse.AuthResponse;
import org.example.project.exception.HttpNotFoundException;
import org.example.project.exception.TokenInValid;
import org.example.project.exception.ValidAlreadyExistsException;
import org.example.project.model.dto.ResetPasswordDto;
import org.example.project.model.dto.ChangePasswordDto;
import org.example.project.model.entity.PasswordResetToken;
import org.example.project.model.entity.RefreshToken;
import org.example.project.model.entity.TokenBlackList;
import org.example.project.model.entity.User;
import org.example.project.model.dto.LoginRequestDto;
import org.example.project.model.dto.RegisterRequestDto;
import org.example.project.repository.PasswordResetTokenRepository;
import org.example.project.repository.RefreshTokenRepository;
import org.example.project.repository.TokenBlackListRepository;
import org.example.project.repository.UserRepository;
import org.example.project.securiry.jwt.JwtUtils;
import org.example.project.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class IUserServiceImpl implements UserService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final TokenBlackListRepository  tokenBlackListRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public User registerUser(RegisterRequestDto registerRequestDto) {
        String Password = passwordEncoder.encode(registerRequestDto.getPassword());
        registerRequestDto.setPassword(Password);
        if (userRepository.existsByEmail(registerRequestDto.getEmail())) {
            throw new ValidAlreadyExistsException("email", "Email Already Exists");
        }
        if (userRepository.existsByUsername(registerRequestDto.getUsername())) {
            throw new ValidAlreadyExistsException("username", "Username Already Exists");
        }
        if (userRepository.existsByPhoneNumber(registerRequestDto.getPhoneNumber())) {
            throw new ValidAlreadyExistsException("phoneNumber", "PhoneNumber Already Exists");
        }
        User user = new User(registerRequestDto);
        return userRepository.save(user);
    }

    @Override
    public User repairUser(Long id, RegisterRequestDto dto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new HttpNotFoundException("User not found"));

        if (userRepository.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ValidAlreadyExistsException("email", "Email Already Exists");
        }

        if (userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
            throw new ValidAlreadyExistsException("username", "Username Already Exists");
        }

        if (userRepository.existsByPhoneNumberAndIdNot(dto.getPhoneNumber(), id)) {
            throw new ValidAlreadyExistsException("phoneNumber", "PhoneNumber Already Exists");
        }

        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public Void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new HttpNotFoundException("User not found"));
        user.setIsEnabled(false);
        userRepository.save(user);
        return null;
    }

    @Override
    public Page<User> findAllBySearch(String search, Pageable pageable) {
        return userRepository.findAllBySearch(search, pageable);
    }

    @Override
    public AuthResponse login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByUsername(loginRequestDto.getUsername()).orElseThrow(() -> new HttpNotFoundException("User not found"));
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new HttpNotFoundException("Wrong Password");
        }
        String accessToken = jwtUtils.generateToken(user, 30 * 60 * 1000L);
        String refreshToken = jwtUtils.generateToken(user, 7 * 24 * 60 * 60 * 1000L);
        RefreshToken refresh = refreshTokenRepository
                .findByUser(user)
                .orElse(new RefreshToken());

        refresh.setUser(user);
        refresh.setToken(refreshToken);
        refresh.setRevoked(false);
        refresh.setExpiryDate(
                new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)
        );

        refreshTokenRepository.save(refresh);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String Token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(Token).orElseThrow(() -> new HttpNotFoundException("Refresh Token not found"));
        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().before(new Date())) {
            throw new TokenInValid("Refresh Token Not Revoked");
        }
        User user = refreshToken.getUser();
        String accessToken = jwtUtils.generateToken(user, 30 * 60 * 1000L);
        String newToken = jwtUtils.generateToken(user, 7 * 24 * 60 * 60 * 1000L);
        refreshToken.setToken(newToken);
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, newToken);
    }

    @Override
    public Void Logout(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new HttpNotFoundException("Access Token not found");
        }

        String token = authHeader.substring(7);

        Date exp = jwtUtils.getExp(token);

        LocalDateTime expiryTime = exp.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        String username = jwtUtils.getUsername(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new HttpNotFoundException("User not found"));

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new HttpNotFoundException("Refresh Token not found"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        long ttl = exp.getTime() - System.currentTimeMillis();

        redisTemplate.opsForValue().set(
                token,
                "BLACKLISTED",
                ttl,
                TimeUnit.MILLISECONDS
        );
        tokenBlackListRepository.save(
                TokenBlackList.builder()
                        .token(token)
                        .user(user)
                        .expiryTime(expiryTime)
                        .build()
        );

        return null;
    }

    @Override
    public String forgotPasswordByEmail(String email) {
        User user=userRepository.findByEmail(email).orElseThrow(() -> new HttpNotFoundException("User not found"));
        String token= UUID.randomUUID().toString();
        PasswordResetToken resetToken=PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .build();
        passwordResetTokenRepository.save(resetToken);
        String link="http://localhost:8080/api/v1/reset-password?token="+token;
        return link;
    }

    @Override
    public void resetPassword(ResetPasswordDto resetPasswordDto) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(resetPasswordDto.getToken()).orElseThrow(() -> new HttpNotFoundException("Token not found"));
        if (token.getExpiryDate().before(new Date())) {
            throw new TokenInValid("Token Expired");
        }
        User user = token.getUser();
        user.setPassword(
                passwordEncoder.encode(resetPasswordDto.getNewPassword())
        );
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);
        RefreshToken refreshToken=refreshTokenRepository.findByUser(user).orElseThrow(() -> new HttpNotFoundException("Refresh Token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void changePassword(String username, ChangePasswordDto changePasswordDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new HttpNotFoundException("User not found"));


        if (!passwordEncoder.matches(changePasswordDto.getOldPassword(), user.getPassword())) {
            throw new HttpNotFoundException("Old password is incorrect");
        }

        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            throw new HttpNotFoundException("New password and confirm password do not match");
        }
        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        userRepository.save(user);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new HttpNotFoundException("Refresh Token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}