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
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class ReceiverPartyValidationHandler implements MessageHandler {

    private MessageHandler nextHandler;

    @Value("${fe.validator.nitreceptor}")
    private String nitreceptor;

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        try {
            Document documentXML = message.getDocumentXML();
            if (documentXML != null) {
                String dataReceiverParty = extractSubXML(getStringFromDocument(documentXML), RECEIVER_PARTY_NODE);
                if (dataReceiverParty.length() > 0) {
                    org.dom4j.Document documentReceiverParty = DocumentHelper.parseText(dataReceiverParty);
                    Element rootCompanyID = documentReceiverParty.getRootElement();

                    Node nodeCompanyID = rootCompanyID.selectSingleNode(COMPANYID_ELEMENT);
                    String companyID = (nodeCompanyID == null ? "" : nodeCompanyID.getText());
                    if (!nitreceptor.equalsIgnoreCase(companyID)) {
                        log.error(VAL_RECEIVERPARTY_WRONG.toString());
                        message.getValidationMessages().add(VAL_RECEIVERPARTY_WRONG.toString());
                        applyNextRule = false;
                    }
                } else {
                    log.error(VAL_RECEIVERPARTY_SEG.toString());
                    message.getValidationMessages().add(VAL_RECEIVERPARTY_SEG.toString());
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
