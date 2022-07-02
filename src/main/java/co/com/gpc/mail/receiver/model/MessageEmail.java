/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.model;

import static co.com.gpc.mail.receiver.parserxml.XMLUtil.getStringFromDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.util.MimeMessageParser;
import org.w3c.dom.Document;

/**
 * @author scabrera
 */
@Slf4j
@Data
public class MessageEmail {

    private Message message;
    private List<String> validationMessages;
    private Map<String, Object> attachmentMap;
    private Document documentXML;
    private String documentType;

    public MessageEmail() {
        validationMessages = new ArrayList<>();
    }

    public MessageEmail(Message message, List<String> validationMessages) {
        this.message = message;
        this.validationMessages = validationMessages;
    }

    public MimeMessageParser getMimeMessageParser() {
        final MimeMessage messageToExtract = (MimeMessage) message;
        try {
            return new MimeMessageParser(messageToExtract).parse();
        } catch (Exception ex) {
            log.error("Error get mime message in model MessageEmail", ex);
            return null;
        }
    }
    
    @Override
    public String toString(){
         StringBuilder messageEmail = new StringBuilder();
         try{
             
             messageEmail.append("Email: ").append(message.getSubject() !=null ? message.getSubject(): "");
             messageEmail.append("Validadores: ").append(String.join(", ", validationMessages));
             messageEmail.append("HashMap:: ");
             attachmentMap.entrySet().stream()
            .forEach(entry -> messageEmail.append(entry.getKey()).append(":").append(entry.getValue()));             
             messageEmail.append("XML Document: ").append(documentXML!=null ? getStringFromDocument(documentXML):"");
         }catch(Exception ex){
            log.error("Is not possible read message email clearly.", ex);
            return "";
         }
         return messageEmail.toString();
    }
    
    
    
}
