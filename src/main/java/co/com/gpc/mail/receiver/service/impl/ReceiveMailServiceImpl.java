package co.com.gpc.mail.receiver.service.impl;


import static co.com.gpc.mail.receiver.MainEncrypt.decrypt;
import co.com.gpc.mail.receiver.handler.impl.*;
import co.com.gpc.mail.receiver.model.MessageEmail;
import co.com.gpc.mail.receiver.model.TransportMessage;
import static co.com.gpc.mail.receiver.util.Constants.*;
import co.com.gpc.mail.receiver.service.ReceiveMailService;
import co.com.gpc.mail.receiver.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SearchTerm;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service Logic Business Email receiver and Sender logic
 *
 * @author Sammy
 */
@Slf4j
@Service
public class ReceiveMailServiceImpl implements ReceiveMailService {


   

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
    @Value("${mail.imap.fromdate}")
    private String fromDate;
    
     
    
    //Declaring handlers
    @Autowired
    private SizeMessageHandler sizeMessageHandler;
    @Autowired
    private SubjectMessageHandler subjectMessageHandler;
    @Autowired
    private ShowContentHandler showContentHandler;
    @Autowired
    private DownloadAttachmentFilesHandler downloadAttachmentFilesHandler;
    @Autowired
    private SchemaDIANValidationHandler schemaDIANValidationHandler;
    @Autowired
    private ValidaDSignHandler validaDSignHandler;
    @Autowired
    private ValidResponseDIANHandler validResponseDIANHandler;
    @Autowired
    private ReceiverPartyValidationHandler receiverPartyValidationHandler;
    @Autowired
    private SenderPartyValidationHandler senderPartyValidationHandler;
    
    
    

    @Override
    public void handleReceivedMail(MimeMessage receivedMessage) {
        try {

            
            Folder folder = receivedMessage.getFolder();
            folder.open(Folder.READ_WRITE);

            Date fromDateInBox = Util.convertDateInBox(fromDate);

            // creates a search criterion
            SearchTerm searchCondition = new SearchTerm() {
                @Override
                public boolean match(Message message) {
                    try {
                        if (message.getSentDate().after(fromDateInBox)) {
                            return true;
                        }
                    } catch (MessagingException ex) {
                        log.error("Error occurred during process message(searchCondition)", ex);
                    }
                    return false;
                }
            };
            List<TransportMessage> listTransportMessage = new ArrayList<>();
            Message[] messages = folder.search(searchCondition);
            
            for(int i=0; i < messages.length;i++){
                listTransportMessage.add(new TransportMessage(Arrays.asList(messages).get(i),folder,receivedMessage));
            }
            log.info("Qty messages found " + messages.length);
            fetchMessagesInFolder(folder, messages);
            log.info("Qty messages purged " + messages.length);
            
            listTransportMessage.stream().filter(message-> {
                MimeMessage currentMessage = (MimeMessage) message.getMessage();
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            }).forEach(this::chainValidatorsMail);            

            

            folder.close(true);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void fetchMessagesInFolder(Folder folder, Message[] messages) throws MessagingException {
        FetchProfile contentsProfile = new FetchProfile();
        contentsProfile.add(FetchProfile.Item.ENVELOPE);
        contentsProfile.add(FetchProfile.Item.CONTENT_INFO);
        contentsProfile.add(FetchProfile.Item.FLAGS);
        contentsProfile.add(FetchProfile.Item.SIZE);
        folder.fetch(messages, contentsProfile);
    }

    private void copyMailToDownloadedFolder(MimeMessage mimeMessage, Folder folder) throws MessagingException {
        Store store = folder.getStore();
        Folder downloadedMailFolder = store.getFolder(DOWNLOADED_MAIL_FOLDER);
        if (downloadedMailFolder.exists()) {
            downloadedMailFolder.open(Folder.READ_WRITE);
            downloadedMailFolder.appendMessages(new MimeMessage[]{mimeMessage});
            downloadedMailFolder.close();
        }
    }
    
    
    
private void copyMailToRejectedFolder(MimeMessage mimeMessage, Folder folder) throws MessagingException {
        Store store = folder.getStore();
        Folder downloadedMailFolder = store.getFolder(REJECTED_MAIL_FOLDER);
        if (downloadedMailFolder.exists()) {
            downloadedMailFolder.open(Folder.READ_WRITE);
            downloadedMailFolder.appendMessages(new MimeMessage[]{mimeMessage});
            downloadedMailFolder.close();
        }
    }
    
     
    
    

    private void chainValidatorsMail(TransportMessage message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message.getMessage();
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(messageToExtract).parse();
            
            if(mimeMessageParser!=null && message!=null){
               MessageEmail messageEmail = new MessageEmail();
               messageEmail.setMessage(message.getMessage());
                //Declaring chain
               sizeMessageHandler.setNextCHandler(subjectMessageHandler);
               subjectMessageHandler.setNextCHandler(showContentHandler);
               showContentHandler.setNextCHandler(downloadAttachmentFilesHandler);
               downloadAttachmentFilesHandler.setNextCHandler(schemaDIANValidationHandler);
               schemaDIANValidationHandler.setNextCHandler(validaDSignHandler);    
               validaDSignHandler.setNextCHandler(validResponseDIANHandler);               
               validResponseDIANHandler.setNextCHandler(receiverPartyValidationHandler);               
               receiverPartyValidationHandler.setNextCHandler(senderPartyValidationHandler);
               
               //Calling the first node of the chain
               sizeMessageHandler.validate(messageEmail);
               if(messageEmail.getValidationMessages().size() > 0){
                   //Printing the output of the chain of handlers by email
                   rejectEmailRedirect(mimeMessageParser, messageEmail.getValidationMessages());
                   copyMailToRejectedFolder(message.getReceivedMessage(), message.getFolder());
               }else{
                   emailRedirect(mimeMessageParser);
                   copyMailToDownloadedFolder(message.getReceivedMessage(), message.getFolder());
               }
                // To delete downloaded email               
                messageToExtract.setFlag(Flags.Flag.DELETED, true);
                deleteFileDownloaded(mimeMessageParser);  
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    private void createDirectoryIfNotExists(String directoryPath) {
        if (!Files.exists(Paths.get(directoryPath))) {
            try {
                Files.createDirectories(Paths.get(directoryPath));
            } catch (IOException e) {
                log.error("An error occurred during create folder: {}", directoryPath, e);
            }
        }
    }
    
    
    private void rejectEmailRedirect(MimeMessageParser mimeMessageParser, List<String> validators) {

        try {
            final String username = senderEmail.replace("%40", "@");
            final String password =  decrypt(senderPassword,secretkey);

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
            message.setSubject("[REJECT] "+mimeMessageParser.getSubject());

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(mimeMessageParser.getPlainContent());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            mimeMessageParser.getAttachmentList().forEach(dataSource -> {
                if (StringUtils.isNotBlank(dataSource.getName())) {
                    String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                    String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER;
                    createDirectoryIfNotExists(dataFolderPath);

                    String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + dataSource.getName();
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
            messageBodyPart = new MimeBodyPart();            
            validators.forEach((validateMessage) -> {
                try {
                    File myObj = new File(VALIDATOR_NAME_FILE);
                    myObj.createNewFile();
                    FileWriter myWriter = new FileWriter(VALIDATOR_NAME_FILE);                    
                    myWriter.write(validateMessage+ "\n");
                    myWriter.close(); 
                    
                } catch (IOException ex) {
                    log.error("Error to save file validators",ex);
                }
            }); 
            DataSource source = new FileDataSource(VALIDATOR_NAME_FILE);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(VALIDATOR_NAME_FILE);
            multipart.addBodyPart(messageBodyPart);            

            // Send the complete message parts
            message.setContent(multipart);
            Transport.send(message);
            log.debug("Email reject sent");

        } catch (Exception e) {
            log.error("Failed to save file in reject message.", e);
        }
    }    
    

    private void emailRedirect(MimeMessageParser mimeMessageParser) {

        try {
            final String username = senderEmail.replace("%40", "@");
            final String password = decrypt(senderPassword,secretkey);

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
            message.setSubject(mimeMessageParser.getSubject());

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(mimeMessageParser.getPlainContent());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            mimeMessageParser.getAttachmentList().forEach(dataSource -> {
                if (StringUtils.isNotBlank(dataSource.getName())) {
                    String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                    String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER;
                    createDirectoryIfNotExists(dataFolderPath);

                    String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + dataSource.getName();
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

            // Send the complete message parts
            message.setContent(multipart);
            Transport.send(message);
            log.debug("Email sent");

        } catch (Exception e) {
            log.error("Failed to save file.", e);
        }
    }

    private void deleteFileDownloaded(MimeMessageParser mimeMessageParser) {
        try {
            List<DataSource> attachments = mimeMessageParser.getAttachmentList();
            for (DataSource attachment : attachments) {
                if (StringUtils.isNotBlank(attachment.getName())) {
                    String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                    String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER;
                    createDirectoryIfNotExists(dataFolderPath);
                    String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + attachment.getName();
                    File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);
                    if (downloadedAttachmentFile.exists()) {
                        String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath); // returns "zip"                            
                        if (extZip.equals(EXTENSION_ZIP)) {
                            if (downloadedAttachmentFile.delete()) {
                                log.info("Attachment file deleted successfully: {}", downloadedAttachmentFilePath);
                            } else {
                                log.error("Failed to delete the file attachment: {}", downloadedAttachmentFilePath);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete the file attachment ", e);
        }
    }



}
