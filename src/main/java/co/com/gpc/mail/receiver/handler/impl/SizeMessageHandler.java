/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler.impl;

import co.com.gpc.mail.receiver.handler.MessageHandler;
import co.com.gpc.mail.receiver.model.MessageEmail;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class SizeMessageHandler implements MessageHandler {

    private MessageHandler nextHandler;

    @Value("${fe.validator.maxsize}")
    private int maxsize;

    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;

        try {
            int sizeMessage = convertBytesToKb(message.getMessage().getSize());
            if (sizeMessage > maxsize) {
                log.error(VAL_OVER_SIZE + " " + sizeMessage);
                message.getValidationMessages().add(VAL_OVER_SIZE.toString());
                applyNextRule = false;
            }
        } catch (MessagingException ex) {
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

    private int convertBytesToKb(int sizeBytes) {
        return (sizeBytes / 1024);
    }

}
