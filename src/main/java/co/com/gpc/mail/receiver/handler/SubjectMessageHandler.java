/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static co.com.gpc.mail.receiver.util.Constants.*;
import static co.com.gpc.mail.receiver.util.MessageCode.*;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Service
public class SubjectMessageHandler implements MessageHandler {
    private MessageHandler nextHandler;

    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectMessageHandler.class);
        
    
    @Override
    public void validate(MessageEmail message) {
        boolean applyNextRule = true;
        
        try{
            String emailSubject = message.getMessage().getSubject();
            List<String> subjectList = new ArrayList<>(Arrays.asList(emailSubject.split(SPLIT_CHAR_SUBJECT)));
            if(subjectList.size() < 4){
                LOGGER.error(VAL_SUBJECT_EST.toString()+" {"+emailSubject+"}");            
                message.getValidationMessages().add(VAL_SUBJECT_EST.toString()+" {"+emailSubject+"}");
                applyNextRule = false;            
            }                   
        }catch(MessagingException ex){
            message.getValidationMessages().add(VAL_MESSAGE.toString()+ex.getMessage());
            LOGGER.error(VAL_MESSAGE.toString(),ex);            
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
    
    
    
}
