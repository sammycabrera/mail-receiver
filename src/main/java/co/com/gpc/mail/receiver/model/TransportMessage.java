/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package co.com.gpc.mail.receiver.model;

import java.util.Objects;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import lombok.Data;

/**
 *
 * @author Sammy
 */
@Data
public class TransportMessage {
    
    private Message message;
    private Folder folder;
    private MimeMessage receivedMessage;

    public TransportMessage() {
    }

    public TransportMessage(Message message, Folder folder, MimeMessage receivedMessage) {
        this.message = message;
        this.folder = folder;
        this.receivedMessage = receivedMessage;
    }

    
    
}
