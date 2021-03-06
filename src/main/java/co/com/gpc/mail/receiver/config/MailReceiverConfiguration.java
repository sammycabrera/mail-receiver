package co.com.gpc.mail.receiver.config;

import co.com.gpc.mail.receiver.service.ReceiveMailService;
import co.com.gpc.mail.receiver.util.security.UtilSecurity;
import lombok.extern.slf4j.Slf4j;
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
    
    @Value("${mail.imap.password}")
    private String imapPassword;
    @Value("${jasypt.encryptor.password}")
    private String secretkey;
    @Value("${jasypt.encryptor.algorithm}")    
    private String algorithm;
    @Value("${jasypt.encryptor.iv-generator-classname}")        
    private String ivgeneratorclassname;
    @Value("${mail.imap.maxfetchsize}")
    private int maxfetchsize;


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
        String imapUrl =storeUrl.replace(imapPassword, UtilSecurity.decrypt(imapPassword,secretkey, algorithm,ivgeneratorclassname));
        log.info("IMAP connection url: {}", storeUrl);
        log.info("IMAP URL connection url: {}", imapUrl);

        ImapMailReceiver imapMailReceiver = new ImapMailReceiver(imapUrl);
        imapMailReceiver.setShouldMarkMessagesAsRead(true);
        imapMailReceiver.setShouldDeleteMessages(false);
        imapMailReceiver.setMaxFetchSize(maxfetchsize);
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
