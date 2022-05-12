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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Service
public class ValidResponseDIANHandler implements MessageHandler {

    private MessageHandler nextHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidResponseDIANHandler.class);

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        Map<String, Object> attachmentMap = new HashMap<>();
        try {
            attachmentMap = message.getAttachmentMap();
            if (attachmentMap != null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                org.w3c.dom.Document document = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(attachmentMap.get(XML_CONTENT).toString().getBytes("utf-8")));
                org.dom4j.io.DOMReader reader = new DOMReader();
                org.dom4j.Document document4j = reader.read(document);                
                String dataResponse = extractSubXML(document4j.asXML(), "cac:Response");
                if (dataResponse.length() > 0) {
                    org.dom4j.Document documentResponse = DocumentHelper.parseText(dataResponse);
                    Element rootResponse = documentResponse.getRootElement();

                    Node nodeResponse = rootResponse.selectSingleNode("//cbc:ResponseCode");
                    String responseCode = (nodeResponse == null ? "" : nodeResponse.getText());
                    nodeResponse = rootResponse.selectSingleNode("//cbc:Description");
                    String responseDesc = (nodeResponse == null ? "" : nodeResponse.getText());

                    if (!RESPONSE_CODE_OK.equalsIgnoreCase(responseCode)) {
                        LOGGER.error(VAL_VALID_DIAN.toString());
                        LOGGER.error("Estado documento (ResponseCode) " + responseCode);
                        LOGGER.error("Estado documento (Description) " + responseDesc);
                        message.getValidationMessages().add(VAL_VALID_DIAN.toString());
                        applyNextRule = false;
                    }
                } else {
                    LOGGER.error(VAL_VALID_DIAN_SEG.toString());
                    message.getValidationMessages().add(VAL_VALID_DIAN_SEG.toString());
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
