package natsi.sn.applestore.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:Apple Store}")
    private String appName;

    public void sendOtpCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Code de vérification - " + appName);
        message.setText(buildOtpEmailBody(code));
        mailSender.send(message);
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

