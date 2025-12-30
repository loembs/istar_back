package natsi.sn.applestore.services;

import lombok.RequiredArgsConstructor;
import natsi.sn.applestore.data.models.OtpCode;
import natsi.sn.applestore.data.repository.OtpCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final EmailService emailService;
    private final Random random = new Random();

    public String generateAndSendOtp(String email, String purpose) {
        // Invalider les anciens codes
        otpCodeRepository.invalidateAllCodesForEmail(email, purpose);

        // Générer un nouveau code à 6 chiffres
        String code = String.format("%06d", random.nextInt(1000000));

        // Créer et sauvegarder le code OTP
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setCode(code);
        otpCode.setPurpose(purpose);
        otpCode.setCreatedAt(LocalDateTime.now());
        otpCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otpCode.setUsed(false);

        otpCodeRepository.save(otpCode);

        // Envoyer l'email
        emailService.sendOtpCode(email, code);

        return code;
    }

    public boolean verifyOtp(String email, String code, String purpose) {
        Optional<OtpCode> otpOptional = otpCodeRepository.findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(
                email, code, LocalDateTime.now());

        if (otpOptional.isPresent()) {
            OtpCode otp = otpOptional.get();
            if (otp.getPurpose().equals(purpose) && otp.isValid()) {
                otp.setUsed(true);
                otpCodeRepository.save(otp);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void cleanupExpiredCodes() {
        otpCodeRepository.deleteExpiredCodes(LocalDateTime.now());
    }
}

