/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.mail.util.MimeMessageParser;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class ShowContentHandler implements MessageHandler {
    private MessageHandler nextHandler;

        
    
    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        
        try{
            showMailContent(message.getmimeMessageParser());                  
        }catch(Exception ex){
            message.getValidationMessages().add(EMAIL_SHOW_CONTENT.toString()+" "+ex.getMessage());
            log.error(EMAIL_SHOW_CONTENT.toString(),ex);            
            applyNextRule = false;          
        }
        
        
        //Pass to next handler
        if(applyNextRule){
            if(nextHandler!=null){
                nextHandler.validate(message);
            }
        }
    }

    @Override
    public void setNextCHandler(MessageHandler handler) {
        nextHandler = handler;
    }   
    
    private void showMailContent(MimeMessageParser mimeMessageParser) throws Exception {
        log.debug("From: {} to: {} | Subject: {}", mimeMessageParser.getFrom(), mimeMessageParser.getTo(), mimeMessageParser.getSubject());
        log.debug("Mail content: {}", mimeMessageParser.getPlainContent());

    }    
    
}
