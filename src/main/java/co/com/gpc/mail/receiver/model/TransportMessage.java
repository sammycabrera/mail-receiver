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

/**
 *
 * @author Sammy
 */
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

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public MimeMessage getReceivedMessage() {
        return receivedMessage;
    }

    public void setReceivedMessage(MimeMessage receivedMessage) {
        this.receivedMessage = receivedMessage;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.message);
        hash = 23 * hash + Objects.hashCode(this.folder);
        hash = 23 * hash + Objects.hashCode(this.receivedMessage);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransportMessage other = (TransportMessage) obj;
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        if (!Objects.equals(this.folder, other.folder)) {
            return false;
        }
        if (!Objects.equals(this.receivedMessage, other.receivedMessage)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "TransportMessage{" + "message=" + message + ", folder=" + folder + ", receivedMessage=" + receivedMessage + '}';
    }
    
    
    
}
