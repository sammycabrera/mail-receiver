package co.com.gpc.mail.receiver.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SearchTerm;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;

/**
 * Service Logic Business Email receiver and Sender logic
 * @author Sammy
 */
@Service
public class ReceiveMailServiceImpl implements ReceiveMailService {

    private static final Logger log = LoggerFactory.getLogger(ReceiveMailServiceImpl.class);

    private static final String DOWNLOAD_FOLDER = "data";

    private static final String DOWNLOADED_MAIL_FOLDER = "DOWNLOADED";
    
    private static final String XML_CONTENT = "XMLContent";
    
    private static final String XML_PART = "XMLPart";
    
    private static final String PDF_PART = "PDFPart";
    
    private static final String XML_FILE = "XMLFile";
    
    private static final String EXTENSION_ZIP = "zip";
    
    private static final String SPLIT_CHAR = ",";
    
    

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
    @Value("${mail.imap.fromdate}")
    private String fromDate;     
    @Value("${fe.validator.schemafile}")
    private String schemaFile;         
    
    @Override
    public void handleReceivedMail(MimeMessage receivedMessage) {
        try {

            Folder folder = receivedMessage.getFolder();
            folder.open(Folder.READ_WRITE);

            Date fromDateInBox = convertDateInBox(fromDate);
            
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
            
            Message[] messages = folder.search(searchCondition);
            log.info("Qty messages found "+messages.length);
            fetchMessagesInFolder(folder, messages);
            log.info("Qty messages purged "+messages.length);
            Arrays.asList(messages).stream().filter(message -> {
                MimeMessage currentMessage = (MimeMessage) message;
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            }).forEach(this::extractMail);

            copyMailToDownloadedFolder(receivedMessage, folder);

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

    private void extractMail(Message message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message;
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(messageToExtract).parse();

            showMailContent(mimeMessageParser);
            downloadAttachmentFiles(mimeMessageParser);
            Map<String, Object> attachmentMap = readAttachment(mimeMessageParser);
            if(attachmentMap!=null){
                if(Boolean.TRUE.equals(attachmentMap.get(XML_PART))){
                    log.info("Xml present in Attachment."+attachmentMap.get(XML_FILE).toString());
                    boolean valid = validateSchemaDIAN(attachmentMap.get(XML_FILE).toString(), schemaFile);
                    if(valid){
                        log.info("Valid electronic document according to DIAN Schema UBL 2.1");
                        
                        emailRedirect(mimeMessageParser);
                        // To delete downloaded email
                        messageToExtract.setFlag(Flags.Flag.DELETED, true);
                        deleteFileDownloaded(mimeMessageParser);                         
                    }                   
                }else{
                    log.error("Message email not content xml file.");
                }

            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void showMailContent(MimeMessageParser mimeMessageParser) throws Exception {
        log.debug("From: {} to: {} | Subject: {}", mimeMessageParser.getFrom(), mimeMessageParser.getTo(), mimeMessageParser.getSubject());
        log.debug("Mail content: {}", mimeMessageParser.getPlainContent());        
               
    }

    private void downloadAttachmentFiles(MimeMessageParser mimeMessageParser) {
        log.debug("Email has {} attachment files", mimeMessageParser.getAttachmentList().size());
        mimeMessageParser.getAttachmentList().forEach(dataSource -> {
            if (StringUtils.isNotBlank(dataSource.getName())) {
                String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER;
                createDirectoryIfNotExists(dataFolderPath);

                String downloadedAttachmentFilePath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER + File.separator + dataSource.getName();
                File downloadedAttachmentFile = new File(downloadedAttachmentFilePath);

                log.info("Save attachment file to: {}", downloadedAttachmentFilePath);
                String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath); // returns "zip"                            
                if (extZip.equals(EXTENSION_ZIP)) {
                    try (
                            OutputStream out = new FileOutputStream(downloadedAttachmentFile)
                            // InputStream in = dataSource.getInputStream()
                    ) {
                        InputStream in = dataSource.getInputStream();
                        IOUtils.copy(in, out);
                    } catch (IOException e) {
                        log.error("Failed to save file.", e);
                    }
                }
            }
        });
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
    
    
    private void emailRedirect(MimeMessageParser mimeMessageParser) {

        try {
            final String username = senderEmail.replace("%40", "@");  
            final String password = senderPassword;            
                    
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
    
    private Date convertDateInBox(String strDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(strDate);
            return date;
        } catch (ParseException e) {
            log.error(": La fecha no se puede transformar: "
                    + strDate);
            throw new RuntimeException("La fecha no se puede transformar: "
                    + strDate);
        }
    }

    
    private boolean validateSchemaDIAN(String xmlFile, String schemaFile) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {

            List<String> xsdList = new ArrayList<>(Arrays.asList(schemaFile.split(SPLIT_CHAR)));
            Source[] sourceList = new Source[xsdList.size()];

            for (int i = 0; i < xsdList.size(); i++) {
                sourceList[i] = new StreamSource(getClass().getClassLoader().getResource(xsdList.get(i).trim()).toExternalForm());
            }
            Schema schema = schemaFactory.newSchema(sourceList);

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlFile)));
            return true;
        } catch (SAXException | IOException e) {
            log.error("Error to validate schema invoice", e);
            return false;
        }
    }

    private String getResource(String filename) throws FileNotFoundException {
        URL resource = getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
    }    
    
    

    
    private Map<String, Object> readAttachment(MimeMessageParser mimeMessageParser) throws DocumentException {
        Map<String, Object> result = new HashMap<>();
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
                        String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath);
                        if (extZip.equals(EXTENSION_ZIP)) {
                            //path zip file
                            File carpetaExtraer = new File(dataFolderPath);

                            //validate if folder exists
                            if (carpetaExtraer.exists()) {
                                //iterate files inside folder
                                File[] ficheros = carpetaExtraer.listFiles();

                                for (File fichero : ficheros) {
                                    if (attachment.getName().equalsIgnoreCase(fichero.getName())) {

                                        try {
                                            //create temporal buffer to file to unzip
                                            ZipInputStream zis = new ZipInputStream(new FileInputStream(dataFolderPath + File.separator + fichero.getName()));
                                            ZipEntry salida;

                                            while (null != (salida = zis.getNextEntry())) {
                                                FileOutputStream fos = new FileOutputStream(dataFolderPath + File.separator + salida.getName());
                                                int leer;
                                                byte[] buffer = new byte[1024];
                                                while (0 < (leer = zis.read(buffer))) {
                                                    fos.write(buffer, 0, leer);
                                                }
                                                fos.close();
                                                zis.closeEntry();

                                                String ext1 = FilenameUtils.getExtension(dataFolderPath + File.separator + salida.getName()); // returns "txt"

                                                if (ext1.equals("xml")) {
                                                    log.info("Found xml file");
                                                    SAXReader sax = new SAXReader();// Crea un objeto SAXReader
                                                    File xmlFile = new File(dataFolderPath + File.separator + salida.getName());// Crea un objeto de archivo de acuerdo con la ruta especificada
                                                    Document document = sax.read(xmlFile);// Obtenga el objeto del documento, si el documento no tiene nodos, se lanzará una excepción para finalizar antes
                                                    result.put(XML_CONTENT,document.asXML());
                                                    result.put(XML_PART,true);
                                                    result.put(XML_FILE,dataFolderPath + File.separator + salida.getName());
                                                }
                                                if (ext1.equals("pdf")) {   
                                                    log.info("Found pdf file");
                                                    result.put(PDF_PART,true);
                                                }                                                
                                            }
                                        } catch (FileNotFoundException e) {
                                            log.error("Zip folder not found [" + dataFolderPath + "]", e);
                                            throw new RuntimeException("Zip folder not found [" + dataFolderPath + "]" + e.getMessage());
                                        } catch (IOException e) {
                                            log.error("Read zip files filed [" + dataFolderPath + "]", e);
                                            throw new RuntimeException("Read zip files filed [" + dataFolderPath + "]" + e.getMessage());
                                        }
                                    }
                                }
                                log.info("Output directory: " + dataFolderPath);
                            } else {
                                log.error("Not exists folder to extract zip [" + dataFolderPath + "]");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete the file attachment ", e);
        }                  
        
        return result;
    }

    
    
    

}
