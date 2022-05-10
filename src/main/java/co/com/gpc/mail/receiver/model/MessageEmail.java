/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import org.apache.commons.mail.util.MimeMessageParser;

/**
 *
 * @author scabrera
 */
public class MessageEmail {
    
    private Message message;

    private List<String> validationMessages;   
    private Map<String, Object> attachmentMap;

    public MessageEmail() {
        validationMessages = new ArrayList<>();
    }

    
    public MessageEmail(Message message, List<String> validationMessages) {
        this.message = message;
        this.validationMessages = validationMessages;
    }

    
    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public List<String> getValidationMessages() {
        return validationMessages;
    }

    public void setValidationMessages(List<String> validationMessages) {
        this.validationMessages = validationMessages;
    }

    public Map<String, Object> getAttachmentMap() {
        return attachmentMap;
    }

    public void setAttachmentMap(Map<String, Object> attachmentMap) {
        this.attachmentMap = attachmentMap;
    }
    
    
    
    
    
    public MimeMessageParser getmimeMessageParser(){
        final MimeMessage messageToExtract = (MimeMessage) message;
        try {
            return new MimeMessageParser(messageToExtract).parse();
        } catch (Exception ex) {
            return null;
        }    
    }
    
    
}
