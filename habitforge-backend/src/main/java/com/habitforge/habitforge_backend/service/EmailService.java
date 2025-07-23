package com.habitforge.habitforge_backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserService userService;  // Inject UserService for storing/verifying codes

    @Value("${spring.mail.username}")
    private String fromAddress;

    // Send verification email with code (for email verification or login verification)
    public void sendVerificationCode(String email) {
        String code = generateCode();
        // Store the code with the user/email (e.g., in User entity as verificationCode)
        userService.saveVerificationCode(email, code);
        sendVerificationEmail(email, code);
    }

    // Verify the code matches for given email
    public boolean verifyCode(String email, String code) {
        return userService.verifyEmailCode(email, code);
    }

    // Send verification email helper
    public void sendVerificationEmail(String to, String code) {
        String subject = "Verify Your Email for HabitForge";
        String content = String.format("""
                <p>Hello,</p>
                <p>Use the following code to verify your email address:</p>
                <h2>%s</h2>
                <p>If you did not request this, please ignore this email.</p>
                """, code);

        sendHtmlEmail(to, subject, content);
    }

    // Send password reset email
    public void sendPasswordResetEmail(String to, String code) {
        String subject = "Reset Your HabitForge Password";
        String content = String.format("""
                <p>Hello,</p>
                <p>You requested to reset your password. Use the following code:</p>
                <h2>%s</h2>
                <p>If you didn’t request a password reset, you can safely ignore this email.</p>
                """, code);

        sendHtmlEmail(to, subject, content);
    }

    // General HTML email sender
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email to " + to);
        }
    }

    // Simple code generator (6 digit numeric code)
    private String generateCode() {
        int code = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(code);
    }
}



