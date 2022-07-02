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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class ValidResponseDIANHandler implements MessageHandler {

    private MessageHandler nextHandler;

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;

        try {
            Document documentXML = message.getDocumentXML();
            if (documentXML != null) {
                String documentStr = getStringFromDocument(documentXML);
                String dataResponse = extractSubXML(documentStr, RESPONSE_NODE);
                if (dataResponse.length() > 0) {
                    org.dom4j.Document documentResponse = DocumentHelper.parseText(dataResponse);
                    Element rootResponse = documentResponse.getRootElement();

                    Node nodeResponse = rootResponse.selectSingleNode(RESPONSE_CODE_ELEMENT);
                    String responseCode = (nodeResponse == null ? "" : nodeResponse.getText());
                    nodeResponse = rootResponse.selectSingleNode(RESPONSE_DESC_ELEMENT);
                    String responseDesc = (nodeResponse == null ? "" : nodeResponse.getText());

                    if (!documentStr.contains(RESPONSE_DESC_OK)) {
                        log.error(VAL_VALID_DIAN.toString());
                        log.error("Estado documento (ResponseCode) " + responseCode);
                        log.error("Estado documento (Description) " + responseDesc);
                        message.getValidationMessages().add(VAL_VALID_DIAN.toString());
                        applyNextRule = false;
                    }
                } else {
                    log.error(VAL_VALID_DIAN_SEG.toString());
                    message.getValidationMessages().add(VAL_VALID_DIAN_SEG.toString());
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
