/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;
import static co.com.gpc.mail.receiver.util.Constants.*;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import co.com.gpc.mail.receiver.validatexml.XMLValDSign;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class ValidaDSignHandler implements MessageHandler {

    private MessageHandler nextHandler;


    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        Map<String, Object> attachmentMap = new HashMap<>();
        try {
            attachmentMap = message.getAttachmentMap();
            if (attachmentMap != null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                org.w3c.dom.Document docu = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(attachmentMap.get(XML_CONTENT).toString().getBytes("utf-8")));
                boolean resp = XMLValDSign.validateXmlDSig(docu);
                if (!resp) {
                    log.error(VAL_DSIGNATURE.toString());
                    message.getValidationMessages().add(VAL_DSIGNATURE.toString());
                    applyNextRule = false;
                }
            } else {
                log.error(VAL_NOT_XML.toString());
                message.getValidationMessages().add(VAL_NOT_XML.toString());
                applyNextRule = false;
            }
        } catch (Exception ex) {
            message.getValidationMessages().add(VAL_MESSAGE.toString() + ex.getMessage());
            log.error(VAL_MESSAGE.toString(), ex);
            applyNextRule = false;
        }

        //Pass to next handler
        if (applyNextRule) {
            if (nextHandler != null) {
                nextHandler.validate(message);
            }
        }
    }

    @Override
    public void setNextCHandler(MessageHandler handler) {
        nextHandler = handler;
    }

}
