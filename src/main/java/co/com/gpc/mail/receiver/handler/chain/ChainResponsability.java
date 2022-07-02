/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.handler.chain;

import co.com.gpc.mail.receiver.handler.impl.DownloadAttachmentFilesHandler;
import co.com.gpc.mail.receiver.handler.impl.ReceiverPartyValidationHandler;
import co.com.gpc.mail.receiver.handler.impl.SchemaDIANValidationHandler;
import co.com.gpc.mail.receiver.handler.impl.SenderPartyValidationHandler;
import co.com.gpc.mail.receiver.handler.impl.ShowContentHandler;
import co.com.gpc.mail.receiver.handler.impl.SizeMessageHandler;
import co.com.gpc.mail.receiver.handler.impl.SubjectMessageHandler;
import co.com.gpc.mail.receiver.handler.impl.ValidResponseDIANHandler;
import co.com.gpc.mail.receiver.handler.impl.ValidaDSignHandler;
import co.com.gpc.mail.receiver.model.MessageEmail;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author scabrera
 */
@Slf4j
@Service
public class ChainResponsability {
    
    //Declaring handlers
    @Autowired
    private SizeMessageHandler sizeMessageHandler;
    @Autowired
    private SubjectMessageHandler subjectMessageHandler;
    @Autowired
    private ShowContentHandler showContentHandler;
    @Autowired
    private DownloadAttachmentFilesHandler downloadAttachmentFilesHandler;
    @Autowired
    private SchemaDIANValidationHandler schemaDIANValidationHandler;
    @Autowired
    private ValidaDSignHandler validaDSignHandler;
    @Autowired
    private ValidResponseDIANHandler validResponseDIANHandler;
    @Autowired
    private ReceiverPartyValidationHandler receiverPartyValidationHandler;
    @Autowired
    private SenderPartyValidationHandler senderPartyValidationHandler;




    @PostConstruct
    private void chainDefinition() {
        try {          
                //Declaring chain
               sizeMessageHandler.setNextCHandler(subjectMessageHandler);
               subjectMessageHandler.setNextCHandler(showContentHandler);
               showContentHandler.setNextCHandler(downloadAttachmentFilesHandler);
               downloadAttachmentFilesHandler.setNextCHandler(schemaDIANValidationHandler);
               schemaDIANValidationHandler.setNextCHandler(validaDSignHandler);    
               validaDSignHandler.setNextCHandler(validResponseDIANHandler);               
               validResponseDIANHandler.setNextCHandler(receiverPartyValidationHandler);               
               receiverPartyValidationHandler.setNextCHandler(senderPartyValidationHandler);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }   
    
    
    public void runChainOfResponsability(MessageEmail messageEmail){
        //Calling the first node of the chain
        sizeMessageHandler.validate(messageEmail);
    }
    
    
    
    
    
}
