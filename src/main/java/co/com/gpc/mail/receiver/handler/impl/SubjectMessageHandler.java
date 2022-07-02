/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler.impl;

import co.com.gpc.mail.receiver.handler.MessageHandler;
import co.com.gpc.mail.receiver.model.MessageEmail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import static co.com.gpc.mail.receiver.util.Constants.*;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class SubjectMessageHandler implements MessageHandler {

    private MessageHandler nextHandler;

    @Value("${fe.validator.codetype}")
    private String codetype;

    @Value("${fe.validator.posCodeFE}")
    private int posCodeFE;

    @Value("${fe.validator.posCodeEvent}")
    private int posCodeEvent;

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        String documentType = "";
        try {
            String emailSubject = message.getMessage().getSubject();
            List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
            if (subjectList.size() < 4) {
                log.error(VAL_SUBJECT_EST + " {" + emailSubject + "}");
                message.getValidationMessages().add(VAL_SUBJECT_EST + " {" + emailSubject + "}");
                applyNextRule = false;
            } else {
                List<String> codetypeList = new ArrayList<>(Arrays.asList(codetype.split(SPLIT_CHAR)));
                codetypeList.replaceAll(String::trim);
                if (subjectList.size() > posCodeFE  && subjectList.get(posCodeFE) != null && codetypeList.contains(subjectList.get(posCodeFE))) {
                    documentType = subjectList.get(posCodeFE);
                }
                if (documentType.length() == 0 && subjectList.size() > posCodeEvent  && subjectList.get(posCodeEvent) != null
                    && codetypeList.contains(subjectList.get(posCodeEvent))) {
                    documentType = subjectList.get(posCodeEvent);
                }

                if (documentType.length() == 0) {
                    log.error(VAL_SUBJECT_EST + " {" + emailSubject + "}");
                    message.getValidationMessages().add(VAL_SUBJECT_EST + " {" + emailSubject + "}");
                    applyNextRule = false;
                }

            }
        } catch (MessagingException ex) {
            message.getValidationMessages().add(VAL_MESSAGE + ex.getMessage());
            log.error(VAL_MESSAGE.toString(), ex);
            applyNextRule = false;
        }

        //Pass to next handler
        if (applyNextRule && nextHandler != null) {
            message.setDocumentType(documentType);
            log.debug("Sent message next handler ", message);
            nextHandler.validate(message);
        }
    }

    @Override
    public void setNextCHandler(MessageHandler handler) {
        nextHandler = handler;
    }

}
