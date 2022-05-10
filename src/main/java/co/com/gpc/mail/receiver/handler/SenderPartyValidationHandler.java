/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;
import static co.com.gpc.mail.receiver.util.Constants.*;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import co.com.gpc.mail.receiver.util.Util;
import static co.com.gpc.mail.receiver.validatexml.XMLValDSign.extractSubXML;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Service
public class SenderPartyValidationHandler implements MessageHandler {

    private MessageHandler nextHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(SenderPartyValidationHandler.class);

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        Map<String, Object> attachmentMap = new HashMap<>();
        try {
            attachmentMap = message.getAttachmentMap();
            if (attachmentMap != null) {
                String emailSubject = message.getMessage().getSubject();
                List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
                if (subjectList.size() > 0) {
                    SAXReader sax = new SAXReader();
                    org.dom4j.Document document = sax.read(new File(Util.getResource(attachmentMap.get(XML_FILE).toString())));
                    String dataSenderParty = extractSubXML(document.asXML(), "cac:SenderParty");
                    if (dataSenderParty.length() > 0) {
                        org.dom4j.Document documentSenderParty = DocumentHelper.parseText(dataSenderParty);
                        Element rootCompanyIDSender = documentSenderParty.getRootElement();

                        Node nodeCompanyIDSender = rootCompanyIDSender.selectSingleNode("//cbc:CompanyID");
                        String CompanyIDSender = (nodeCompanyIDSender == null ? "" : nodeCompanyIDSender.getText());
                        if (!CompanyIDSender.equalsIgnoreCase(subjectList.get(0))) {
                            LOGGER.error(VAL_SENDERPARTY_WRONG.toString() + " Nit {" + subjectList.get(0) + "} Emisor {" + CompanyIDSender + "} ");
                            message.getValidationMessages().add(VAL_SENDERPARTY_WRONG.toString() + " Nit {" + subjectList.get(0) + "} Emisor {" + CompanyIDSender + "} ");
                            applyNextRule = false;
                        }
                    } else {
                        LOGGER.error(VAL_SENDERPARTY_SEG.toString());
                        message.getValidationMessages().add(VAL_SENDERPARTY_SEG.toString());
                        applyNextRule = false;
                    }
                } else {
                    LOGGER.error(VAL_SUBJECT_EST.toString());
                    message.getValidationMessages().add(VAL_SUBJECT_EST.toString());
                    applyNextRule = false;
                }
            } else {
                LOGGER.error(VAL_NOT_XML.toString());
                message.getValidationMessages().add(VAL_NOT_XML.toString());
                applyNextRule = false;
            }
        } catch (Exception ex) {
            message.getValidationMessages().add(VAL_MESSAGE.toString() + ex.getMessage());
            LOGGER.error(VAL_MESSAGE.toString(), ex);
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
