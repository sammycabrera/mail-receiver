/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler.impl;

import co.com.gpc.mail.receiver.handler.MessageHandler;
import co.com.gpc.mail.receiver.model.MessageEmail;
import static co.com.gpc.mail.receiver.parserxml.XMLUtil.*;
import static co.com.gpc.mail.receiver.util.Constants.*;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import co.com.gpc.mail.receiver.util.Util;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class SchemaDIANValidationHandler implements MessageHandler {

    private MessageHandler nextHandler;

    @Value("${fe.validator.schemafile}")
    private String schemaFile;

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        Map<String, Object> attachmentMap = new HashMap<>();
        Document documentXML = null;
        try {
            attachmentMap = Util.readAttachment(message.getMimeMessageParser());
            if (attachmentMap != null) {
                if (!Boolean.TRUE.equals(attachmentMap.get(XML_PART))) {
                    log.error(VAL_NOT_XML.toString());
                    message.getValidationMessages().add(VAL_NOT_XML.toString());
                    applyNextRule = false;
                } else {
                    boolean valid = Util.validateSchemaDIAN(attachmentMap.get(XML_FILE).toString(), schemaFile);
                    if (!valid) {
                        log.error(VAL_INVALID_SCHEMAXML.toString());
                        message.getValidationMessages().add(VAL_INVALID_SCHEMAXML.toString());
                        applyNextRule = false;
                    } else {
                        documentXML = convertStringToDocument(attachmentMap.get(XML_CONTENT).toString());
                    }
                }
            } else {
                log.error(VAL_NOT_XML.toString());
                message.getValidationMessages().add(VAL_NOT_XML.toString());
                applyNextRule = false;
            }
        } catch (Exception ex) {
            message.getValidationMessages().add(VAL_MESSAGE + ex.getMessage());
            log.error(VAL_MESSAGE.toString(), ex);
            applyNextRule = false;
        }

        //Pass to next handler
        if (applyNextRule && nextHandler != null) {
            message.setAttachmentMap(attachmentMap);
            message.setDocumentXML(documentXML);
            log.debug("Sent message next handler ", message);
            nextHandler.validate(message);
        }
    }

    @Override
    public void setNextCHandler(MessageHandler handler) {
        nextHandler = handler;
    }

}
