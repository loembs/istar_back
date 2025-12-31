package natsi.sn.applestore.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.name:Apple Store}")
    private String appName;

    public void sendOtpCode(String to, String code) {
        try {
            // Vérifier que la configuration email est complète
            if (fromEmail == null || fromEmail.isEmpty() ||
                    mailSender == null) {
                log.warn("Email service not configured. OTP code for {}: {}", to, code);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Code de vérification - " + appName);
            message.setText(buildOtpEmailBody(code));
            mailSender.send(message);
            log.info("OTP code sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
            // Ne pas faire échouer l'application si l'email échoue
            // L'OTP est toujours généré et stocké, même si l'email n'est pas envoyé
        }
    }

    private String buildOtpEmailBody(String code) {
        return "Bonjour,\n\n" +
                "Votre code de vérification est : " + code + "\n\n" +
                "Ce code est valide pendant 10 minutes.\n\n" +
                "Si vous n'avez pas demandé ce code, ignorez cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe " + appName;
    }
}

