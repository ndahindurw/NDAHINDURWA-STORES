package com.webtech.ndahindurwastores.service;

import com.webtech.ndahindurwastores.model.ResetToken;
import com.webtech.ndahindurwastores.model.Role;
import com.webtech.ndahindurwastores.model.User;
import com.webtech.ndahindurwastores.userRepository.ResetTokenRepository;
import com.webtech.ndahindurwastores.userRepository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResetTokenRepository resetTokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    @PostConstruct
    public void init() {
        addAdminUser(); // Automatically add the admin user on application startup
    }

    @Transactional
    public boolean sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }

        deleteExistingResetTokenByEmail(email);

        String token = UUID.randomUUID().toString();
        saveResetTokenForUser(user, token);

        String resetUrl = "http://localhost:8080/reset-password?token=" + token;
        String message = "To reset your password, click the link below:\n" + resetUrl;
        sendEmail(email, "Password Reset", message);

        return true;
    }

    @Transactional
    public void saveResetTokenForUser(User user, String token) {
        ResetToken resetToken = new ResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));

        resetTokenRepository.save(resetToken);
    }

    @Transactional
    public void deleteExistingResetTokenByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            resetTokenRepository.findByUser(user).ifPresent(resetToken -> {
                resetTokenRepository.delete(resetToken);
                System.out.println("Deleted existing token: " + resetToken.getToken());
            });
        }
    }

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public boolean doesEmailExist(String email) {
        return userRepository.findByEmail(email) != null;
    }

    @Transactional
    public User registerUser(User user) {
        return userRepository.save(user);
    }

    public User loginUser(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findUserByResetToken(String token) {
        return resetTokenRepository.findByToken(token)
                .map(ResetToken::getUser);
    }

    public boolean validatePasswordResetToken(String token) {
        Optional<ResetToken> resetTokenOptional = resetTokenRepository.findByToken(token);
        if (resetTokenOptional.isPresent()) {
            ResetToken resetToken = resetTokenOptional.get();
            boolean isValid = resetToken.getExpiryDate().isAfter(LocalDateTime.now());
            System.out.println("Token: " + token + ", Valid: " + isValid);
            return isValid;
        }
        System.out.println("Token: " + token + " not found.");
        return false;
    }

    @Transactional
    public boolean resetUserPassword(String token, String newPassword) {
        if (!validatePasswordResetToken(token)) {
            System.out.println("Invalid or expired token: " + token);
            return false;
        }

        Optional<User> userOptional = findUserByResetToken(token);
        if (!userOptional.isPresent()) {
            System.out.println("No user found for the token: " + token);
            return false;
        }

        User user = userOptional.get();
        user.setPassword(newPassword);
        userRepository.save(user);

        resetTokenRepository.deleteByToken(token);

        return true;
    }

    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            userRepository.deleteById(id);
            System.out.println("User with ID " + id + " deleted successfully.");
        } else {
            System.out.println("User with ID " + id + " not found.");
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> searchUsers(String username, String email) {
        return userRepository.findByUsernameContainingOrEmailContaining(username, email);
    }

    public void addAdminUser() {
        if (userRepository.findByUsername("admin") == null) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword("admin");
            adminUser.setEmail("mahoromichaela@gmail.com");
            adminUser.setRole(Role.ROLE_ADMIN);

            userRepository.save(adminUser);
            System.out.println("Admin user created successfully.");
        } else {
            System.out.println("Admin user already exists.");
        }
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        System.out.println("Retrieved users: " + users);
        return users;
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public void saveAll(List<User> userList) {
        userRepository.saveAll(userList);
    }
}
