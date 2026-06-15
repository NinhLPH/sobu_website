package com.vn.sodu.mail;

import com.vn.sodu.user.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpEmailServiceTest {

    @Test
    @DisplayName("Should send activation email to registered address with frontend activation link")
    void sendActivationEmailUsesRegisteredEmailAndFrontendActivationLink() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailService service = new SmtpEmailService(mailSender);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@sobu.test");

        Account account = new Account();
        account.setEmail("newuser@example.com");
        account.setFullName("New User");

        service.sendActivationEmail(account, "token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@sobu.test", message.getFrom());
        assertArrayEquals(new String[]{"newuser@example.com"}, message.getTo());
        assertEquals("Activate your Sobu account", message.getSubject());
        assertTrue(message.getText().contains("http://localhost:5173/activate?token=token-123"));
    }

    @Test
    @DisplayName("Should dispatch activation email on mail executor")
    void sendActivationEmailUsesMailExecutor() throws Exception {
        Async async = SmtpEmailService.class
                .getMethod("sendActivationEmail", Account.class, String.class)
                .getAnnotation(Async.class);

        assertNotNull(async);
        assertEquals("mailExecutor", async.value());
    }
}
