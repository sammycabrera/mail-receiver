/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler.impl;

import co.com.gpc.mail.receiver.handler.MessageHandler;
import co.com.gpc.mail.receiver.model.MessageEmail;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static co.com.gpc.mail.receiver.util.Constants.*;
import co.com.gpc.mail.receiver.util.Util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.springframework.core.io.FileSystemResource;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import org.springframework.stereotype.Service;
/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class DownloadAttachmentFilesHandler  implements MessageHandler {
    private MessageHandler nextHandler;

        
    
    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        
        try{
            downloadAttachmentFiles(message.getMimeMessageParser());
        }catch(Exception ex){
            message.getValidationMessages().add(ERROR_DOWNLOAD_FILES.toString()+" "+ex.getMessage());
            log.error(ERROR_DOWNLOAD_FILES.toString(),ex);
            applyNextRule = false;          
        }
        
        
        //Pass to next handler
        if(applyNextRule){
            if(nextHandler!=null){
                nextHandler.validate(message);
            }
        }
    }

    @Override
    public void setNextCHandler(MessageHandler handler) {
        nextHandler = handler;
    }   
    
    
    private void downloadAttachmentFiles(MimeMessageParser mimeMessageParser) {
        log.debug("Email has {} attachment files", mimeMessageParser.getAttachmentList().size());
        mimeMessageParser.getAttachmentList().forEach(dataSource -> {
            if (StringUtils.isNotBlank(dataSource.getName())) {
                String rootDirectoryPath = new FileSystemResource("").getFile().getAbsolutePath();
                String dataFolderPath = rootDirectoryPath + File.separator + DOWNLOAD_FOLDER;
                Util.createDirectoryIfNotExists(dataFolderPath);

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
                        throw new RuntimeException("Failed to save file.", e);
                    }
                }
            }
        });
    }    
}
