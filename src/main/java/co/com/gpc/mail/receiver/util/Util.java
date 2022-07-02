/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.util;

import co.com.gpc.mail.receiver.exception.ErrorCodes;
import co.com.gpc.mail.receiver.exception.ReadZipFileException;
import co.com.gpc.mail.receiver.exception.ZipFolderNotFoundException;
import static co.com.gpc.mail.receiver.util.Constants.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.springframework.core.io.FileSystemResource;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

/**
 * @author scabrera
 */
@Slf4j
public class Util {

 

    public static void createDirectoryIfNotExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("An error occurred during create folder: {}", directoryPath, e);
            }
        }
    }

    public static Map<String, Object> readAttachment(MimeMessageParser mimeMessageParser) {
        Map<String, Object> result = new HashMap<>();
        try {

            List<DataSource> attachments = mimeMessageParser.getAttachmentList();
            for (DataSource attachment : attachments) {
                if (StringUtils.isNotBlank(attachment.getName())) {
                    String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                    
                    StringBuilder dataFolderPath = getDataFolderPath();
                    
                    StringBuilder downloadedAttachmentFilePath = new StringBuilder();

                    downloadedAttachmentFilePath.append(rootDirectoryPath).append(File.separator).
                            append(DOWNLOAD_FOLDER).append(File.separator).append(attachment.getName()).toString();

                    
                    File downloadedAttachmentFile = new File(downloadedAttachmentFilePath.toString());
                    if (downloadedAttachmentFile.exists()) {
                        String extZip = FilenameUtils.getExtension(downloadedAttachmentFilePath.toString());
                        if (extZip.equals(EXTENSION_ZIP)) {
                            //path zip file
                            File carpetaExtraer = new File(dataFolderPath.toString());

                            //validate if folder exists
                            if (carpetaExtraer.exists()) {
                                //iterate files inside folder
                                File[] ficheros = carpetaExtraer.listFiles();

                                for (File fichero : ficheros) {
                                    if (attachment.getName().equalsIgnoreCase(fichero.getName())) {

                                        StringBuilder dataFolderPathFileOut = new StringBuilder();
                                        dataFolderPathFileOut.append(dataFolderPath.toString()).append(File.separator).append(fichero.getName());
                                        try(ZipInputStream zis = new ZipInputStream(new FileInputStream(dataFolderPathFileOut.toString()))) {
                                            
                                            ZipEntry salida;

                                            while (null != (salida = zis.getNextEntry())) {
                                                StringBuilder dataFolderPathFileSal = new StringBuilder();
                                                dataFolderPathFileSal.append(dataFolderPath.toString()).append(File.separator).append(salida.getName());
                                                
                                                try(FileOutputStream fos = new FileOutputStream(dataFolderPathFileSal.toString());){
                                                    int leer;
                                                    byte[] buffer = new byte[1024];
                                                    while (0 < (leer = zis.read(buffer))) {
                                                        fos.write(buffer, 0, leer);
                                                    }
                                                    zis.closeEntry();                                                    
                                                }

                                                String ext1 = FilenameUtils.getExtension(dataFolderPathFileSal.toString()); 

                                                if (ext1.equals("xml")) {
                                                    log.info("Found xml file");
                                                    SAXReader sax = new SAXReader();// Crea un objeto SAXReader
                                                    File xmlFile = new File(dataFolderPathFileSal.toString());// Crea un objeto de archivo de acuerdo con la ruta especificada
                                                    Document document = sax.read(xmlFile);// Obtenga el objeto del documento, si el documento no tiene nodos, se lanzará una excepción para finalizar antes
                                                    result.put(XML_CONTENT, document.asXML());
                                                    result.put(XML_PART, true);
                                                    result.put(XML_FILE, dataFolderPathFileSal.toString());
                                                }
                                                if (ext1.equals("pdf")) {
                                                    log.info("Found pdf file");
                                                    result.put(PDF_PART, true);
                                                }
                                            }
                                        } catch (FileNotFoundException e) {
                                            log.error("Zip folder not found [" + dataFolderPath + "]", e);
                                            throw new ZipFolderNotFoundException("Zip folder not found [" + dataFolderPath + "]" + e.getMessage(), e, ErrorCodes.ZIP_FOLDER_NOT_FOUND);
                                        } catch (IOException e) {
                                            log.error("Read zip files filed [" + dataFolderPath + "]", e);
                                            throw new ReadZipFileException("Read zip files filed [" + dataFolderPath + "]" + e.getMessage(), e, ErrorCodes.READ_ZIP_ERROR);
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
            log.error("Failed to read the file attachment ", e);
            throw new ReadZipFileException("Failed to read the file attachment " + e.getMessage(), e, ErrorCodes.READ_ZIP_ERROR);
        }

        return result;
    }


    public static StringBuilder getDataFolderPath(){
        String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
        StringBuilder dataFolderPath = new StringBuilder();
        dataFolderPath.append(rootDirectoryPath).append(File.separator).append(DOWNLOAD_FOLDER);
        Util.createDirectoryIfNotExists(dataFolderPath.toString());
        return dataFolderPath;
    }


    public static boolean validateSchemaDIAN(String xmlFile, String schemaFile) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {

            List<String> xsdList = new ArrayList<>(Arrays.asList(schemaFile.split(SPLIT_CHAR)));
            Source[] sourceList = new Source[xsdList.size()];
            Util util = new Util();
            for (int i = 0; i < xsdList.size(); i++) {
                sourceList[i] = new StreamSource(util.getClass().getClassLoader().getResource(xsdList.get(i).trim()).toExternalForm());
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

    public static String getResource(String filename) {
        Util util = new Util();
        URL resource = util.getClass().getClassLoader().getResource(filename);
        Objects.requireNonNull(resource);

        return resource.getFile();
    }


    public static Date convertDateInBox(String strDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_YYYY_MM_DD);
            return sdf.parse(strDate);
        } catch (ParseException e) {
            log.error(": La fecha no se puede transformar: "
                    + strDate);
            throw new ReadZipFileException("La fecha no se puede transformar: "
                    + strDate + e.getMessage(), e, ErrorCodes.FORMAT_DATE_ERROR);
        }
    }
    
    


}
