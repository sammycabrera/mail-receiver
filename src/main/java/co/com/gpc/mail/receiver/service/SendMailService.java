/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.service;

import java.util.List;
import org.apache.commons.mail.util.MimeMessageParser;
/**
 *
 * @author scabrera
 */
public interface SendMailService {
    
    void sendEmailInternalInbox(MimeMessageParser mimeMessageParser, List<String> validators);
    
}
