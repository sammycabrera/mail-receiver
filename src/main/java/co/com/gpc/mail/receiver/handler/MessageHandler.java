package co.com.gpc.mail.receiver.handler;

import co.com.gpc.mail.receiver.model.MessageEmail;

public interface MessageHandler {

    public void validate(MessageEmail message);

    public void setNextCHandler(MessageHandler handler);
}
