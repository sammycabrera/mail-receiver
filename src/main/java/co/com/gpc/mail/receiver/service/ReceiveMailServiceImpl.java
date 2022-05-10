package co.com.gpc.mail.receiver.service;

import co.com.gpc.mail.receiver.handler.DownloadAttachmentFilesHandler;
import co.com.gpc.mail.receiver.handler.ReceiverPartyValidationHandler;
import co.com.gpc.mail.receiver.handler.SchemaDIANValidationHandler;
import co.com.gpc.mail.receiver.handler.SenderPartyValidationHandler;
import co.com.gpc.mail.receiver.handler.ShowContentHandler;
import co.com.gpc.mail.receiver.handler.SizeMessageHandler;
import co.com.gpc.mail.receiver.handler.SubjectMessageHandler;
import co.com.gpc.mail.receiver.handler.ValidResponseDIANHandler;
import co.com.gpc.mail.receiver.handler.ValidaDSignHandler;
import co.com.gpc.mail.receiver.model.MessageEmail;
import static co.com.gpc.mail.receiver.util.Constants.VALIDATOR_NAME_FILE;
import co.com.gpc.mail.receiver.util.Util;
import co.com.gpc.mail.receiver.validatexml.XMLValDSign;
import static co.com.gpc.mail.receiver.validatexml.XMLValDSign.extractSubXML;
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
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.xml.sax.SAXException;

/**
 * Service Logic Business Email receiver and Sender logic
 *
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
    
    private static final String SPLIT_CHAR_SUBJECT = ";";
    
    private static final String RESPONSE_CODE_OK = "02";

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
    @Value("${fe.validator.nitreceptor}")
    private String nitreceptor;    
    @Value("${fe.validator.maxsize}")
    private int maxsize;       
    
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

            Message[] messages = folder.search(searchCondition);
            log.info("Qty messages found " + messages.length);
            fetchMessagesInFolder(folder, messages);
            log.info("Qty messages purged " + messages.length);
            Arrays.asList(messages).stream().filter(message -> {
                MimeMessage currentMessage = (MimeMessage) message;
                try {
                    return currentMessage.getMessageID().equalsIgnoreCase(receivedMessage.getMessageID());
                } catch (MessagingException e) {
                    log.error("Error occurred during process message", e);
                    return false;
                }
            //}).forEach(this::extractMail);
            }).forEach(this::chainValidatorsMail);            

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
            int sizeMessage = convertBytesToKb(message.getSize());
            if(sizeMessage > maxsize){
                 log.error("Email over size "+sizeMessage);
            }else{                                
                String emailSubject = message.getSubject();
                List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
                if(subjectList.size() < 4){
                    log.error("Error subject structure found {"+emailSubject+"}");
                }else{               
                    showMailContent(mimeMessageParser);
                    downloadAttachmentFiles(mimeMessageParser);
                    Map<String, Object> attachmentMap = readAttachment(mimeMessageParser);
                    if (attachmentMap != null) {
                        if (Boolean.TRUE.equals(attachmentMap.get(XML_PART))) {
                            log.info("Xml present in Attachment." + attachmentMap.get(XML_FILE).toString());
                            boolean valid = validateSchemaDIAN(attachmentMap.get(XML_FILE).toString(), schemaFile);
                            if (valid) {
                                log.info("Valid electronic document according to DIAN Schema UBL 2.1");
                                
                                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                dbf.setNamespaceAware(true);     
                                org.w3c.dom.Document docu =dbf.newDocumentBuilder().parse(new FileInputStream(getResource(attachmentMap.get(XML_FILE).toString())));
                                boolean resp =XMLValDSign.validateXmlDSig(docu );
                                if(!resp){
                                   log.error("Invalid digital signature electronic document");
                                }else{
                                    log.info("Valid digital signature electronic document");
                                    SAXReader sax = new SAXReader();
                                    Document document = sax.read(new File(getResource(attachmentMap.get(XML_FILE).toString())));
                                    String dataResponse =  extractSubXML(document.asXML(), "cac:Response") ;
                                    if(dataResponse.length() > 0){
                                        Document documentResponse = DocumentHelper.parseText(dataResponse);
                                        Element rootResponse = documentResponse.getRootElement();

                                        Node nodeResponse = rootResponse.selectSingleNode("//cbc:ResponseCode");
                                        String responseCode = (nodeResponse == null ? "" : nodeResponse.getText());                
                                        nodeResponse = rootResponse.selectSingleNode("//cbc:Description");
                                        String responseDesc = (nodeResponse == null ? "" : nodeResponse.getText());

                                        if(RESPONSE_CODE_OK.equalsIgnoreCase(responseCode)){
                                            log.info("Estado documento (ResponseCode) "+responseCode);
                                            log.info("Estado documento (Description) "+responseDesc);

                                            String dataReceiverParty =  extractSubXML(document.asXML(), "cac:ReceiverParty") ;
                                            if(dataReceiverParty.length() > 0){
                                                Document documentReceiverParty = DocumentHelper.parseText(dataReceiverParty);
                                                Element rootCompanyID = documentReceiverParty.getRootElement();

                                                Node nodeCompanyID = rootCompanyID.selectSingleNode("//cbc:CompanyID");
                                                String CompanyID = (nodeCompanyID == null ? "" : nodeCompanyID.getText());
                                                if(nitreceptor.equalsIgnoreCase(CompanyID)){
                                                    log.info("ReceiverParty OK "+CompanyID);

                                                    String dataSenderParty =  extractSubXML(document.asXML(), "cac:SenderParty") ;
                                                    if(dataSenderParty.length() > 0){
                                                        Document documentSenderParty = DocumentHelper.parseText(dataSenderParty);
                                                        Element rootCompanyIDSender = documentSenderParty.getRootElement();

                                                        Node nodeCompanyIDSender = rootCompanyIDSender.selectSingleNode("//cbc:CompanyID");
                                                        String CompanyIDSender = (nodeCompanyIDSender == null ? "" : nodeCompanyIDSender.getText());
                                                        if(CompanyIDSender.equalsIgnoreCase( subjectList.get(0) )){
                                                            log.info("SenderParty OK "+CompanyIDSender);
                                                            emailRedirect(mimeMessageParser);    
                                                        }else{
                                                            log.error("Wrong SenderParty Subject {"+subjectList.get(0)+"} SenderParty {"+CompanyIDSender+"} ");
                                                        }
                                                    }                                    
                                                }else {
                                                    log.error("Wrong ReceiverParty "+CompanyID);
                                                }
                                            }

                                        }else{
                                            log.error("No valido (Description) "+responseDesc);
                                        }
                                    }   
                                }
                            }
                        } else {
                            log.error("Message email not content xml file.");
                        }
                    }  
                    
                }              
            }

            // To delete downloaded email
            messageToExtract.setFlag(Flags.Flag.DELETED, true);
            deleteFileDownloaded(mimeMessageParser);             
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }    
    
    

    private void chainValidatorsMail(Message message) {
        try {
            final MimeMessage messageToExtract = (MimeMessage) message;
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(messageToExtract).parse();
            
            if(mimeMessageParser!=null && message!=null){
               MessageEmail messageEmail = new MessageEmail();
               messageEmail.setMessage(message);
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

               }else{
                   emailRedirect(mimeMessageParser);
               }
                // To delete downloaded email               
                messageToExtract.setFlag(Flags.Flag.DELETED, true);
                deleteFileDownloaded(mimeMessageParser);  
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
                            OutputStream out = new FileOutputStream(downloadedAttachmentFile) // InputStream in = dataSource.getInputStream()
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
    
    
    private void rejectEmailRedirect(MimeMessageParser mimeMessageParser, List<String> validators) {

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
                                                    result.put(XML_CONTENT, document.asXML());
                                                    result.put(XML_PART, true);
                                                    result.put(XML_FILE, dataFolderPath + File.separator + salida.getName());
                                                }
                                                if (ext1.equals("pdf")) {
                                                    log.info("Found pdf file");
                                                    result.put(PDF_PART, true);
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
//
//    public static boolean nsRegister(String ns, ArrayList<String> list) {
//        if (list != null) {
//            if (list.contains(ns)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public static String extractNamespace(String xmlFile) {
//        Pattern p = Pattern.compile("xmlns:[^=]+=\"[^\"]+\"");
//        Matcher m = p.matcher(xmlFile);
//        StringBuilder data = new StringBuilder();
//        ArrayList<String> list = new ArrayList<>();
//        while (m.find()) {
//            if (!nsRegister(m.group(), list)) {
//                data.append(m.group());
//                data.append(" ");
//                list.add(m.group());
//            }
//
//        }
//        return data.toString();
//    }
//
//    public static String extractSubXML(String fileXml, String tagName)
//            throws DocumentException {
//        String nameSpacesXml = extractNamespace(fileXml);
//        System.out.println(nameSpacesXml);
//        if (fileXml.contains("<" + tagName)) {
//            int beginPos = fileXml.indexOf("<" + tagName);
//            int endPos = fileXml.indexOf("</" + tagName + ">");
//            String subXml = "<Documento " + nameSpacesXml + "> " + fileXml.substring(beginPos + tagName.length(), endPos) + " </Documento>";
//            return (subXml == null ? "" : subXml);
//        }
//        return "";
//    }
    
    
    private int convertBytesToKb(int size_bytes) {
        int size_kb = (size_bytes / 1024);
        return size_kb;
    }

}
