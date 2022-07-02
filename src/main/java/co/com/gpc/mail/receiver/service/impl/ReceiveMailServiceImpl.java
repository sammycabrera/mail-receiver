package co.com.gpc.mail.receiver.service.impl;


import co.com.gpc.mail.receiver.handler.chain.ChainResponsability;
import co.com.gpc.mail.receiver.handler.impl.*;
import co.com.gpc.mail.receiver.model.MessageEmail;
import co.com.gpc.mail.receiver.model.TransportMessage;
import static co.com.gpc.mail.receiver.util.Constants.*;
import co.com.gpc.mail.receiver.service.ReceiveMailService;
import co.com.gpc.mail.receiver.service.SendMailService;
import co.com.gpc.mail.receiver.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.activation.DataSource;
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

    @Value("${mail.imap.fromdate}")
    private String fromDate;
    
     
        
    @Autowired
    private SendMailService sendMailService;    
    @Autowired
    private ChainResponsability chainResponsability;       
    
    
    

    @Override
    public void handleReceivedMail(MimeMessage receivedMessage) {
        Folder folder = null;
        try {
            
            folder = receivedMessage.getFolder();
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
            log.info("Qty messages found {}", messages.length);
            fetchMessagesInFolder(folder, messages);
            log.info("Qty messages purged {}", messages.length);
            
            listTransportMessage.stream().filter(message-> {
                MimeMessage currentMessage = (MimeMessage) message.getMessage();
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            }).forEach(this::chainValidatorsMail);            
                       

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }finally{
             if (folder != null) {
                try {
                  folder.close(true);
                } catch (final MessagingException ignore) {
                    log.error(ignore.getMessage(), ignore);
                }
              }
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

    
    
    
    private void copyMailToFolder(MimeMessage mimeMessage, Folder folder, List<String> validators) {
        Folder downloadedMailFolder = null;
        try{
            Store store = folder.getStore();
            downloadedMailFolder = store.getFolder((!validators.isEmpty() ? REJECTED_MAIL_FOLDER :DOWNLOADED_MAIL_FOLDER));
            if (downloadedMailFolder.exists()) {
                downloadedMailFolder.open(Folder.READ_WRITE);
                downloadedMailFolder.appendMessages(new MimeMessage[]{mimeMessage});
            }
        }catch(MessagingException e){
            log.error(e.getMessage(), e);
        }finally{
             if (downloadedMailFolder != null) {
                try {
                  downloadedMailFolder.close();
                } catch (final MessagingException ignore) {
                    log.error(ignore.getMessage(), ignore);
                }
              }
        }
    }
    
     
    
    

    private void chainValidatorsMail(TransportMessage message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message.getMessage();
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(messageToExtract).parse();
            
            if(mimeMessageParser!=null){
               MessageEmail messageEmail = new MessageEmail();
               messageEmail.setMessage(message.getMessage());
               
               chainResponsability.runChainOfResponsability(messageEmail);
               
               sendMailService.sendEmailInternalInbox(mimeMessageParser, messageEmail.getValidationMessages());
               copyMailToFolder(message.getReceivedMessage(), message.getFolder(),messageEmail.getValidationMessages());
         
                // To delete downloaded email               
                messageToExtract.setFlag(Flags.Flag.DELETED, true);
                deleteFileDownloaded(mimeMessageParser);  
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


 
    

    private void deleteFileDownloaded(MimeMessageParser mimeMessageParser) {
        try {
            List<DataSource> attachments = mimeMessageParser.getAttachmentList();
            for (DataSource attachment : attachments) {
                if (StringUtils.isNotBlank(attachment.getName())) {
                    StringBuilder dataFolderPath =Util.getDataFolderPath();                    
                    String downloadedAttachmentFilePath = dataFolderPath.append(File.separator).append(attachment.getName()).toString();
                    File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);
                    if (downloadedAttachmentFile.exists()) {
                        String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath); // returns "zip"                            
                        if (extZip.equals(EXTENSION_ZIP)) {
                            Path path = Paths.get(downloadedAttachmentFilePath);
                            cleanUp(path);
                            log.info("Attachment file deleted successfully: {}", downloadedAttachmentFilePath);                            
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete the file attachment ", e);
        }
    }

    public void cleanUp(Path path) throws IOException {
        Files.delete(path);
    }
    

    



}
