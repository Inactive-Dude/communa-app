package com.login.communa.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;
import org.springframework.lang.NonNull;

/**
 * Handles all outbound email delivery for Communa.
 *
 * All public send methods are @Async — they execute on a background thread pool
 * so that Gmail SMTP handshakes (1-3 sec) do NOT block inbound HTTP request threads.
 * This resolves the high-severity issue where synchronous email dispatch was
 * exhausting the Tomcat thread pool under load.
 *
 * Exception handling is done inside each method because the @Async proxy
 * detaches execution from the caller's thread, making the caller's try-catch
 * unable to see SMTP exceptions. Errors are logged here server-side.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends a password-reset email asynchronously.
     * Runs on the Spring async thread pool — does NOT block the caller.
     */
    @Async
    public void sendResetEmail(@NonNull String toEmail, @NonNull String token) {
        String resetUrl = frontendUrl + "/reset-password.html?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:'Segoe UI',Arial,sans-serif;background:#f4f4f4;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 0;">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#1a2a3a;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:#162938;padding:28px 32px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;letter-spacing:1px;">
                          🔒 Communa
                        </h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px;color:#cdd5e0;">
                        <h2 style="margin:0 0 16px;color:#ffffff;font-size:18px;">
                          Password Reset Request
                        </h2>
                        <p style="margin:0 0 24px;line-height:1.6;">
                          We received a request to reset your Communa password.
                          Click the button below to choose a new password.
                          This link will expire in <strong style="color:#ffd700;">1 hour</strong>.
                        </p>
                        <div style="text-align:center;margin:24px 0;">
                          <a href="%s"
                             style="display:inline-block;padding:14px 32px;
                                    background:#2a6496;color:#ffffff;
                                    text-decoration:none;border-radius:8px;
                                    font-size:15px;font-weight:bold;">
                            Reset My Password
                          </a>
                        </div>
                        <p style="margin:24px 0 0;font-size:13px;color:#7f8c9a;line-height:1.6;">
                          If you didn't request a password reset, you can safely ignore this email —
                          your password will remain unchanged.<br><br>
                          If the button above doesn't work, copy and paste this URL into your browser:<br>
                          <a href="%s" style="color:#6ab3d4;word-break:break-all;">%s</a>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 32px;background:#111d28;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#4a5a6a;">
                          © Communa — Saintgits College of Engineering
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(resetUrl, resetUrl, resetUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(Objects.requireNonNull(fromEmail, "fromEmail must not be null"));
            helper.setTo(Objects.requireNonNull(toEmail, "toEmail must not be null"));
            helper.setSubject("Password Reset Request \u2014 Communa");
            helper.setText(Objects.requireNonNull(html, "html must not be null"), true);
            mailSender.send(message);
            logger.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            // Log here because @Async detaches this from the caller's thread;
            // the caller's try-catch cannot see this exception.
            logger.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Sends an email verification link asynchronously.
     * Runs on the Spring async thread pool — does NOT block the caller.
     */
    @Async
    public void sendVerificationEmail(@NonNull String toEmail, @NonNull String token) {
        String verifyUrl = frontendUrl + "/verify-email.html?token=" + token;

        String html = """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;font-family:'Segoe UI',Arial,sans-serif;background:#f4f4f4;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr><td align="center" style="padding:40px 0;">
                  <table width="520" cellpadding="0" cellspacing="0"
                         style="background:#1a2a3a;border-radius:12px;overflow:hidden;">
                    <tr>
                      <td style="background:#162938;padding:28px 32px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;letter-spacing:1px;">
                          ✉️ Communa
                        </h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 32px;color:#cdd5e0;">
                        <h2 style="margin:0 0 16px;color:#ffffff;font-size:18px;">
                          Verify Your Email Address
                        </h2>
                        <p style="margin:0 0 24px;line-height:1.6;">
                          Welcome to Communa! Please verify your email address to activate your account.
                          This link will expire in <strong style="color:#ffd700;">24 hours</strong>.
                        </p>
                        <div style="text-align:center;margin:24px 0;">
                          <a href="%s"
                             style="display:inline-block;padding:14px 32px;
                                    background:#1e7e34;color:#ffffff;
                                    text-decoration:none;border-radius:8px;
                                    font-size:15px;font-weight:bold;">
                            Verify My Email
                          </a>
                        </div>
                        <p style="margin:24px 0 0;font-size:13px;color:#7f8c9a;line-height:1.6;">
                          If you didn't create a Communa account, you can safely ignore this email.<br><br>
                          If the button above doesn't work, copy and paste this URL into your browser:<br>
                          <a href="%s" style="color:#6ab3d4;word-break:break-all;">%s</a>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:20px 32px;background:#111d28;text-align:center;">
                        <p style="margin:0;font-size:12px;color:#4a5a6a;">
                          © Communa — Saintgits College of Engineering
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(verifyUrl, verifyUrl, verifyUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(Objects.requireNonNull(fromEmail, "fromEmail must not be null"));
            helper.setTo(Objects.requireNonNull(toEmail, "toEmail must not be null"));
            helper.setSubject("Verify Your Email \u2014 Communa");
            helper.setText(Objects.requireNonNull(html, "html must not be null"), true);
            mailSender.send(message);
            logger.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            // Log here because @Async detaches this from the caller's thread;
            // the caller's try-catch cannot see this exception.
            logger.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}