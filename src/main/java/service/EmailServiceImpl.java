package service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Override
    @Async
    public void sendSimpleMessage(String to, String subject, String text) {
        // TODO: Integrate JavaMailSender with actual SMTP settings
        System.out.println("DEBUG [Email SENT] To: " + to + " | Subject: " + subject + " | Body: " + text);
    }
}
