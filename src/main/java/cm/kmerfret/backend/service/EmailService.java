package cm.kmerfret.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendWelcomeEmail(String to, String fullName) {
        String subject = "Bienvenue sur KmerFret !";
        String body = buildWelcomeHtml(fullName);
        send(to, subject, body);
    }

    @Async
    public void sendOtpEmail(String to, String fullName, String code) {
        String subject = "Votre code de connexion KmerFret : " + code;
        String body = buildOtpHtml(fullName, code);
        send(to, subject, body);
    }

    @Async
    public void sendDriverApprovalEmail(String to, String fullName, String tempPassword) {
        String subject = "Votre compte KmerFret est prêt !";
        String body = buildApprovalHtml(fullName, to, tempPassword);
        send(to, subject, body);
    }

    @Async
    public void sendDriverRejectionEmail(String to, String fullName, String reason) {
        String subject = "Mise à jour de votre candidature KmerFret";
        String body = buildRejectionHtml(fullName, reason);
        send(to, subject, body);
    }

    @Async
    public void sendPaymentReminderEmail(String to, String fullName, String missionRef, BigDecimal amount) {
        String subject = "Action requise — paiement mission " + missionRef;
        String body = buildPaymentReminderHtml(fullName, missionRef, amount);
        send(to, subject, body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, "KmerFret");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Échec envoi email à {} : {}", to, e.getMessage());
        }
    }

    // ─── Templates HTML ────────────────────────────────────────────────────────

    private String buildWelcomeHtml(String name) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px">
              <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden">
                <div style="background:#1B5E20;padding:32px;text-align:center">
                  <h1 style="color:#fff;margin:0;font-size:28px">KmerFret</h1>
                  <p style="color:#A5D6A7;margin:8px 0 0">Transport de confiance au Cameroun</p>
                </div>
                <div style="padding:32px">
                  <h2 style="color:#1B5E20">Bienvenue, %s !</h2>
                  <p style="color:#424242;line-height:1.6">
                    Votre compte KmerFret est créé. Vous pouvez dès maintenant créer vos premières
                    missions de transport et suivre vos marchandises en temps réel.
                  </p>
                  <p style="color:#424242;line-height:1.6">
                    Si vous avez des questions, notre équipe est disponible via l'application.
                  </p>
                  <p style="color:#757575;font-size:13px;margin-top:32px">
                    L'équipe KmerFret — Douala, Cameroun
                  </p>
                </div>
              </div>
            </body></html>
            """.formatted(name);
    }

    private String buildOtpHtml(String name, String code) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px">
              <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden">
                <div style="background:#1B5E20;padding:32px;text-align:center">
                  <h1 style="color:#fff;margin:0">KmerFret</h1>
                </div>
                <div style="padding:32px">
                  <h2 style="color:#1B5E20">Code de connexion</h2>
                  <p style="color:#424242">Bonjour %s, voici votre code de vérification :</p>
                  <div style="background:#E8F5E9;border-radius:8px;padding:24px;text-align:center;margin:24px 0">
                    <span style="font-size:40px;font-weight:700;color:#1B5E20;letter-spacing:12px">%s</span>
                  </div>
                  <p style="color:#757575;font-size:13px">
                    Ce code expire dans <strong>10 minutes</strong>.
                    Si vous n'avez pas tenté de vous connecter, ignorez cet email.
                  </p>
                </div>
              </div>
            </body></html>
            """.formatted(name, code);
    }

    private String buildApprovalHtml(String name, String email, String tempPassword) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px">
              <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden">
                <div style="background:#1B5E20;padding:32px;text-align:center">
                  <h1 style="color:#fff;margin:0">KmerFret</h1>
                </div>
                <div style="padding:32px">
                  <h2 style="color:#1B5E20">Candidature approuvée !</h2>
                  <p style="color:#424242">Félicitations %s ! Votre candidature a été validée.</p>
                  <p style="color:#424242">Vos identifiants de connexion :</p>
                  <div style="background:#E8F5E9;border-radius:8px;padding:20px;margin:16px 0">
                    <p style="margin:4px 0"><strong>Email :</strong> %s</p>
                    <p style="margin:4px 0"><strong>Mot de passe temporaire :</strong> %s</p>
                  </div>
                  <p style="color:#E65100;font-size:13px">
                    ⚠️ Changez votre mot de passe lors de votre première connexion.
                  </p>
                </div>
              </div>
            </body></html>
            """.formatted(name, email, tempPassword);
    }

    private String buildRejectionHtml(String name, String reason) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px">
              <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden">
                <div style="background:#1B5E20;padding:32px;text-align:center">
                  <h1 style="color:#fff;margin:0">KmerFret</h1>
                </div>
                <div style="padding:32px">
                  <h2 style="color:#424242">Mise à jour de votre candidature</h2>
                  <p style="color:#424242">Bonjour %s,</p>
                  <p style="color:#424242">Après examen de votre dossier, nous ne pouvons pas donner suite
                  à votre candidature pour le moment.</p>
                  %s
                  <p style="color:#424242">Vous pouvez soumettre une nouvelle candidature après correction
                  des points mentionnés.</p>
                </div>
              </div>
            </body></html>
            """.formatted(name, reason != null ? "<p style=\"color:#757575\">Motif : " + reason + "</p>" : "");
    }

    private String buildPaymentReminderHtml(String name, String missionRef, BigDecimal amount) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px">
              <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden">
                <div style="background:#E65100;padding:32px;text-align:center">
                  <h1 style="color:#fff;margin:0">KmerFret — Action requise</h1>
                </div>
                <div style="padding:32px">
                  <h2 style="color:#E65100">Paiement requis</h2>
                  <p style="color:#424242">Bonjour %s,</p>
                  <p style="color:#424242">
                    Votre mission <strong>%s</strong> nécessite un premier versement de
                    <strong>%,.0f FCFA</strong> pour être confirmée.
                  </p>
                  <p style="color:#424242">
                    Ouvrez l'application KmerFret et effectuez le paiement depuis le détail de la mission.
                  </p>
                  <p style="color:#757575;font-size:12px">
                    Sans paiement, la mission ne pourra pas démarrer.
                  </p>
                </div>
              </div>
            </body></html>
            """.formatted(name, missionRef, amount);
    }
}
