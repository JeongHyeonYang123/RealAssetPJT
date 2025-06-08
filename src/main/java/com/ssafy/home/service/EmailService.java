package com.ssafy.home.service;

import com.ssafy.home.domain.User;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.domain}")
    private String appDomain;

    public void sendVerificationEmail(User user) {
        try {
            log.info("이메일 발송 시작: {}", user.getEmail());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("회원가입 인증 메일");

            String verificationUrl = appDomain + "/verify?token=" + user.getVerificationToken();
            log.info("인증 URL 생성: {}", verificationUrl);

            String htmlContent = """
                    <html>
                    <body>
                        <h2>회원가입을 완료하려면 아래 버튼을 클릭해주세요</h2>
                        <p>안녕하세요 %s님,</p>
                        <p>회원가입을 완료하려면 아래 링크를 클릭해주세요:</p>
                        <a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
                            회원가입 확인
                        </a>
                        <p>이 링크는 24시간 후 만료됩니다.</p>
                    </body>
                    </html>
                    """
                    .formatted(user.getName(), verificationUrl);

            helper.setText(htmlContent, true);
            log.info("이메일 내용 설정 완료");

            mailSender.send(message);
            log.info("이메일 발송 완료: {}", user.getEmail());

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    public void resendVerificationEmail(User user) {
        sendVerificationEmail(user);
    }
}
