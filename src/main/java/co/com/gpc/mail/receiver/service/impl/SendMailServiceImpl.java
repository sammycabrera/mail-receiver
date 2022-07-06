/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.service.impl;

import co.com.gpc.mail.receiver.exception.DownloadZipException;
import co.com.gpc.mail.receiver.exception.ErrorCodes;
import co.com.gpc.mail.receiver.service.SendMailService;
import static co.com.gpc.mail.receiver.util.Constants.*;
import co.com.gpc.mail.receiver.util.Util;
import co.com.gpc.mail.receiver.util.security.UtilSecurity;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.apache.commons.io.IOUtils;
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
    @Value("${receptor.plcolab-habilitado}")    
    private String destinoPlcolabHabilitado;
    @Value("${receptor.plcolab-email}")    
    private String destinoPlcolab;      
    
    
    @Override
    public void sendEmailInternalInbox(MimeMessageParser mimeMessageParser, List<String> validators) {

        try {
            final String username = senderEmail.replace("%40", "@");
            final String destinoPLColabHab = (destinoPlcolabHabilitado== null || destinoPlcolabHabilitado.length()==0) ? "N" :destinoPlcolabHabilitado.toUpperCase();
            
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
            
            StringBuilder listRecipients = new StringBuilder();
            listRecipients.append(recipientEmail.replace("%40", "@"));
            
            if(destinoPLColabHab.equalsIgnoreCase(VALOR_STRING_VERDADERO) && validators.isEmpty()){
                listRecipients.append(",");
                listRecipients.append(destinoPlcolab.replace("%40", "@"));
            }
            
            
                    
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(listRecipients.toString()));


            //message.setRecipients(Message.RecipientType.TO,
            //        InternetAddress.parse(recipientEmail.replace("%40", "@")));
            message.setSubject((!validators.isEmpty() ? "["+REJECTED_MAIL_FOLDER+"]" :"")+mimeMessageParser.getSubject());                   
            
            String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
            
            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(mimeMessageParser.getPlainContent());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);            

            mimeMessageParser.getAttachmentList().forEach(dataSource -> {
                if (StringUtils.isNotBlank(dataSource.getName())) {
                    
                    
                    StringBuilder dataFolderPath = new StringBuilder();
                    dataFolderPath.append(rootDirectoryPath).append(File.separator).append(DOWNLOAD_FOLDER);
                    Util.createDirectoryIfNotExists(dataFolderPath.toString());
                    String downloadedAttachmentFilePath = dataFolderPath.append(File.separator).append(dataSource.getName()).toString();
                    
                    try {
                        String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath);
                        if (extZip.equals(EXTENSION_ZIP)) {
                            log.info("Save attachment file to: {}", downloadedAttachmentFilePath);
                           
                            File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);
                            if(!downloadedAttachmentFile.exists()){
                                try (
                                        OutputStream out = Files.newOutputStream(downloadedAttachmentFile.toPath());
                                        InputStream in = dataSource.getInputStream()) {
                                    IOUtils.copy(in, out);
                                    log.info("Downloaded file not found: {}", downloadedAttachmentFilePath);
                                } catch (IOException e) {
                                    log.error("Failed to download file to send.", e);
                                    throw new DownloadZipException("Failed to save file.", e, ErrorCodes.DOWNLOAD_ZIP_ERROR);

                                }                            
                            }
                            
                                          
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
                String pattern = VALIDATOR_FORMATDATE_FILE;
                DateFormat df = new SimpleDateFormat(pattern);
                Date today = Calendar.getInstance().getTime(); 
                String todayAsString = df.format(today);
                StringBuilder dataFolderPath = new StringBuilder();
                dataFolderPath.append(rootDirectoryPath).append(File.separator).append(DOWNLOAD_FOLDER);
                StringBuilder fileNameVal = new StringBuilder();
                fileNameVal.append(VALIDATOR_NAME_FILE).append(todayAsString).append(VALIDATOR_EXTENSION_FILE);
                String downloadedValidationTxtFilePath = dataFolderPath.append(File.separator).append(fileNameVal.toString()).toString();
                validators.forEach(validateMessage -> {

                    try(FileWriter myWriter = new FileWriter(downloadedValidationTxtFilePath);) {
                        File myObj = new File(downloadedValidationTxtFilePath);
                        myObj.createNewFile();
                                            
                        myWriter.write(validateMessage+ "\n");

                    } catch (IOException ex) {
                        log.error("Error to save file validators",ex);
                    }
                }); 
                DataSource source = new FileDataSource(downloadedValidationTxtFilePath);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(fileNameVal.toString());
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
