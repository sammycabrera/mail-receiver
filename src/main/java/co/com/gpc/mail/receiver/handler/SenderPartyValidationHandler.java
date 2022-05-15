/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;
import static co.com.gpc.mail.receiver.util.Constants.*;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import static co.com.gpc.mail.receiver.validatexml.XMLValDSign.extractSubXML;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.DOMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        Map<String, Object> attachmentMap = new HashMap<>();
        try {
            attachmentMap = message.getAttachmentMap();
            if (attachmentMap != null) {
                String emailSubject = message.getMessage().getSubject();
                List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
                if (subjectList.size() > 0) {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    org.w3c.dom.Document document = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(attachmentMap.get(XML_CONTENT).toString().getBytes("utf-8")));
                    org.dom4j.io.DOMReader reader = new DOMReader();
                    org.dom4j.Document document4j = reader.read(document);
                    String dataSenderParty = extractSubXML(document4j.asXML(), "cac:SenderParty");
                    if (dataSenderParty.length() > 0) {
                        org.dom4j.Document documentSenderParty = DocumentHelper.parseText(dataSenderParty);
                        Element rootCompanyIDSender = documentSenderParty.getRootElement();

                        Node nodeCompanyIDSender = rootCompanyIDSender.selectSingleNode("//cbc:CompanyID");
                        String CompanyIDSender = (nodeCompanyIDSender == null ? "" : nodeCompanyIDSender.getText());
                        if(!subjectList.get(0).contains(CompanyIDSender)){
                            log.error(VAL_SENDERPARTY_WRONG.toString() + " Nit {" + subjectList.get(0) + "} Emisor {" + CompanyIDSender + "} ");
                            message.getValidationMessages().add(VAL_SENDERPARTY_WRONG.toString() + " Nit {" + subjectList.get(0) + "} Emisor {" + CompanyIDSender + "} ");
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
