/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.util;

import static co.com.gpc.mail.receiver.util.Constants.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;
/**
 *
 * @author scabrera
 */
public class Util {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    
    public static void createDirectoryIfNotExists(String directoryPath) {
        if (!Files.exists(Paths.get(directoryPath))) {
            try {
                Files.createDirectories(Paths.get(directoryPath));
            } catch (IOException e) {
                LOGGER.error("An error occurred during create folder: {}", directoryPath, e);
            }
        }
    }



    public static Map<String, Object> readAttachment(MimeMessageParser mimeMessageParser) throws DocumentException {
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
                                                    LOGGER.info("Found xml file");
                                                    SAXReader sax = new SAXReader();// Crea un objeto SAXReader
                                                    File xmlFile = new File(dataFolderPath + File.separator + salida.getName());// Crea un objeto de archivo de acuerdo con la ruta especificada
                                                    Document document = sax.read(xmlFile);// Obtenga el objeto del documento, si el documento no tiene nodos, se lanzará una excepción para finalizar antes
                                                    result.put(XML_CONTENT, document.asXML());
                                                    result.put(XML_PART, true);
                                                    result.put(XML_FILE, dataFolderPath + File.separator + salida.getName());
                                                }
                                                if (ext1.equals("pdf")) {
                                                    LOGGER.info("Found pdf file");
                                                    result.put(PDF_PART, true);
                                                }
                                            }
                                        } catch (FileNotFoundException e) {
                                            LOGGER.error("Zip folder not found [" + dataFolderPath + "]", e);
                                            throw new RuntimeException("Zip folder not found [" + dataFolderPath + "]" + e.getMessage());
                                        } catch (IOException e) {
                                            LOGGER.error("Read zip files filed [" + dataFolderPath + "]", e);
                                            throw new RuntimeException("Read zip files filed [" + dataFolderPath + "]" + e.getMessage());
                                        }
                                    }
                                }
                                LOGGER.info("Output directory: " + dataFolderPath);
                            } else {
                                LOGGER.error("Not exists folder to extract zip [" + dataFolderPath + "]");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete the file attachment ", e);
        }

        return result;
    }    
 
    
    public static boolean validateSchemaDIAN(String xmlFile, String schemaFile) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {

            List<String> xsdList = new ArrayList<>(Arrays.asList(schemaFile.split(SPLIT_CHAR)));
            Source[] sourceList = new Source[xsdList.size()];
            Util util = new Util();
            for (int i = 0; i < xsdList.size(); i++) {
                sourceList[i] = new StreamSource( util.getClass().getClassLoader().getResource(xsdList.get(i).trim()).toExternalForm());
            }
            Schema schema = schemaFactory.newSchema(sourceList);

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xmlFile)));
            return true;
        } catch (SAXException | IOException e) {
            LOGGER.error("Error to validate schema invoice", e);
            return false;
        }
    }    
    
    public static String getResource(String filename) throws FileNotFoundException {
        Util util = new Util();
        URL resource = util.getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
    }    
    
    
    public static Date convertDateInBox(String strDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(strDate);
            return date;
        } catch (ParseException e) {
            LOGGER.error(": La fecha no se puede transformar: "
                    + strDate);
            throw new RuntimeException("La fecha no se puede transformar: "
                    + strDate);
        }
    }    
    
    
}
