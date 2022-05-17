package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;

public interface MessageHandler {

    void validate(MessageEmail message);

    void setNextCHandler(MessageHandler handler);
}
