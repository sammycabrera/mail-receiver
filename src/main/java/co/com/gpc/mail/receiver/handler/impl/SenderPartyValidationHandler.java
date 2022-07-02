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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class SenderPartyValidationHandler implements MessageHandler {

    private MessageHandler nextHandler;

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        Map<String, Object> attachmentMap;
        try {
            attachmentMap = message.getAttachmentMap();
            if (attachmentMap != null) {
                String emailSubject = message.getMessage().getSubject();
                List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
                if (!subjectList.isEmpty()) {
                    String dataSenderParty = extractSubXML(getStringFromDocument(message.getDocumentXML()), SENDER_PARTY_NODE);
                    if (dataSenderParty.length() > 0) {
                        Document documentSenderParty = DocumentHelper.parseText(dataSenderParty);
                        Element rootCompanyIDSender = documentSenderParty.getRootElement();

                        Node nodeCompanyIDSender = rootCompanyIDSender.selectSingleNode(COMPANYID_ELEMENT);
                        String companyIDSender = (nodeCompanyIDSender == null ? "" : nodeCompanyIDSender.getText());
                        if (!subjectList.get(0).contains(companyIDSender) && !subjectList.get(2).contains(companyIDSender)) {
                            log.error(VAL_SENDERPARTY_WRONG + " Nit no esta como facturador {" + subjectList.get(0) + "} ni como generador de evento {" + subjectList.get(2) + "}  Emisor {" + companyIDSender + "} ");
                            message.getValidationMessages().add(VAL_SENDERPARTY_WRONG + " Nit no esta como facturador {" + subjectList.get(0) + "} ni como generador de evento {" + subjectList.get(2) + "}  Emisor {" + companyIDSender + "}");
                            applyNextRule = false;
                        }
                    } else {
                        log.error(VAL_SENDERPARTY_SEG.toString());
                        message.getValidationMessages().add(VAL_SENDERPARTY_SEG.toString());
                        applyNextRule = false;
                    }
                } else {
                    log.error(VAL_SUBJECT_EST.toString());
                    message.getValidationMessages().add(VAL_SUBJECT_EST.toString());
                    applyNextRule = false;
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
            log.debug("Sent message next handler ", message);
            nextHandler.validate(message);
        }
    }

    @Override
    public void setNextCHandler(MessageHandler handler) {
        nextHandler = handler;
    }

}
