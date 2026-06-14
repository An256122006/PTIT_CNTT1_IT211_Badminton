package org.example.project.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.project.common.reponse.AuthResponse;
import org.example.project.model.dto.ResetPasswordDto;
import org.example.project.model.dto.ChangePasswordDto;
import org.example.project.model.entity.User;
import org.example.project.model.dto.LoginRequestDto;
import org.example.project.model.dto.RegisterRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserService {
    User registerUser(RegisterRequestDto registerRequestDto);
    User repairUser(Long id,RegisterRequestDto registerRequestDto);
    Void deleteUser(Long id);
    Page<User> findAllBySearch(String search, Pageable pageable);
    AuthResponse login(LoginRequestDto loginRequestDto);
    AuthResponse refreshToken(String Token);
    Void Logout(HttpServletRequest request);
    String forgotPasswordByEmail(String email);
    void resetPassword(ResetPasswordDto resetPasswordDto);
    void changePassword(String username, ChangePasswordDto changePasswordDto);
}
