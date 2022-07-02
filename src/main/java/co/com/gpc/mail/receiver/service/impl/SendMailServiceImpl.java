/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.service.impl;

import co.com.gpc.mail.receiver.service.SendMailService;
import static co.com.gpc.mail.receiver.util.Constants.*;
import co.com.gpc.mail.receiver.util.Util;
import co.com.gpc.mail.receiver.util.security.UtilSecurity;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class SendMailServiceImpl implements SendMailService {

    @Value("${receptor.destino}")
    private String recipientEmail;
    @Value("${mail.imap.username}")
    private String senderEmail;
    @Value("${mail.smtp.host}")
    private String senderHost;
    @Value("${mail.smtp.port}")
    private String senderPort;
    @Value("${mail.imap.password}")
    private String senderPassword;    
    @Value("${jasypt.encryptor.password}")
    private String secretkey; 
    @Value("${jasypt.encryptor.algorithm}")    
    private String algorithm;
    @Value("${jasypt.encryptor.iv-generator-classname}")    
    private String ivgeneratorclassname;   
    
    
    @Override
    public void sendEmailInternalInbox(MimeMessageParser mimeMessageParser, List<String> validators) {

        try {
            final String username = senderEmail.replace("%40", "@");
            final String password = UtilSecurity.decrypt(senderPassword,secretkey,algorithm,ivgeneratorclassname);

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", senderHost);
            props.put("mail.smtp.port", senderPort);

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
            session.setDebug(true);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail.replace("%40", "@")));
            message.setSubject("["+(!validators.isEmpty() ? REJECTED_MAIL_FOLDER :DOWNLOADED_MAIL_FOLDER)+"]"+mimeMessageParser.getSubject());

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(mimeMessageParser.getPlainContent());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            mimeMessageParser.getAttachmentList().forEach(dataSource -> {
                if (StringUtils.isNotBlank(dataSource.getName())) {
                    String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                    
                    StringBuilder dataFolderPath = new StringBuilder();
                    dataFolderPath.append(rootDirectoryPath).append(File.separator).append(DOWNLOAD_FOLDER);
                    Util.createDirectoryIfNotExists(dataFolderPath.toString());
                    String downloadedAttachmentFilePath = dataFolderPath.append(File.separator).append(dataSource.getName()).toString();

                    try {
                        String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath);
                        if (extZip.equals(EXTENSION_ZIP)) {
                            log.info("Save attachment file to: {}", downloadedAttachmentFilePath);
                            DataSource source = new FileDataSource(downloadedAttachmentFilePath);
                            final BodyPart messageBodyPartAtt = new MimeBodyPart();
                            messageBodyPartAtt.setDataHandler(new DataHandler(source));
                            messageBodyPartAtt.setFileName(dataSource.getName());
                            multipart.addBodyPart(messageBodyPartAtt);
                        }
                    } catch (MessagingException e) {
                        log.error("Failed to save file.", e);
                    }
                }
            });

            // Part two is attachment
            if(!validators.isEmpty()){
                messageBodyPart = new MimeBodyPart();            
                validators.forEach(validateMessage -> {
                    try(FileWriter myWriter = new FileWriter(VALIDATOR_NAME_FILE);) {
                        File myObj = new File(VALIDATOR_NAME_FILE);
                        myObj.createNewFile();
                                            
                        myWriter.write(validateMessage+ "\n");

                    } catch (IOException ex) {
                        log.error("Error to save file validators",ex);
                    }
                }); 
                DataSource source = new FileDataSource(VALIDATOR_NAME_FILE);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(VALIDATOR_NAME_FILE);
                multipart.addBodyPart(messageBodyPart);            
            }
			

            // Send the complete message parts
            message.setContent(multipart);
            Transport.send(message);
            log.debug("Email sent");

        } catch (Exception e) {
            log.error("Failed to send message to internal inbox.", e);
        }
    }    
    
}
