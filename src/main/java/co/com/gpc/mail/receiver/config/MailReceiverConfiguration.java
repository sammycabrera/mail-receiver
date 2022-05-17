package co.com.gpc.mail.receiver.config;

import co.com.gpc.mail.receiver.service.ReceiveMailService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.messaging.Message;

import javax.mail.internet.MimeMessage;
import java.util.Properties;
/**
 * Config Mail Receiver Spring integration
 * @author Sammy
 */
@Slf4j
@Configuration
@EnableIntegration
public class MailReceiverConfiguration {


    private final ReceiveMailService receiveMailService;

    @Value("${mail.imap.maxfetchsize}")
    private String maxfetchsize;


    public MailReceiverConfiguration(ReceiveMailService receiveMailService) {
        this.receiveMailService = receiveMailService;
    }

    @ServiceActivator(inputChannel = "receiveEmailChannel")
    public void receive(Message<?> message) {
        receiveMailService.handleReceivedMail((MimeMessage) message.getPayload());
    }

    @Bean("receiveEmailChannel")
    public DirectChannel defaultChannel() {
        DirectChannel directChannel = new DirectChannel();
        directChannel.setDatatypes(javax.mail.internet.MimeMessage.class);
        return directChannel;
    }

    @Bean()
    @InboundChannelAdapter(
            channel = "receiveEmailChannel",
            poller = @Poller(fixedDelay = "${mail.imap.fixeddelay}", taskExecutor = "asyncTaskExecutor")
    )
    public MailReceivingMessageSource mailMessageSource(MailReceiver mailReceiver) {
        return new MailReceivingMessageSource(mailReceiver);
    }

    @Bean
    public MailReceiver imapMailReceiver(@Value("imaps://${mail.imap.username}:${mail.imap.password}@${mail.imap.host}:${mail.imap.port}/inbox") String storeUrl) {
        log.info("IMAP connection url: {}", storeUrl);

        ImapMailReceiver imapMailReceiver = new ImapMailReceiver(storeUrl);
        imapMailReceiver.setShouldMarkMessagesAsRead(true);
        imapMailReceiver.setShouldDeleteMessages(false);
        imapMailReceiver.setMaxFetchSize(Integer.parseInt(maxfetchsize));
        imapMailReceiver.setAutoCloseFolder(true);

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        javaMailProperties.put("mail.imap.socketFactory.fallback", false);
        javaMailProperties.put("mail.store.protocol", "imaps");
        javaMailProperties.put("mail.debug", true);

        imapMailReceiver.setJavaMailProperties(javaMailProperties);

        return imapMailReceiver;
    }

}
