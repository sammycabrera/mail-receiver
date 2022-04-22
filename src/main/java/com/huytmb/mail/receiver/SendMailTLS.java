/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.huytmb.mail.receiver;

/**
 *
 * @author Sammy
 */
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMailTLS {

	public static void main(String[] args) {
		String host = "smtp.office365.com";
		int port = 587;
		String username = "recepcionfacturaelectronicasprcexterno@outlook.com";
		String password = "sebastiansaul2020";

		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.ssl.enable", "true");
		Session session = Session.getInstance(props);

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("recepcionfacturaelectronicasprc@outlook.es"));
			message.setSubject("Testing Subject");
			message.setText("Dear Mail Crawler," +
					"\n\n No spam to my email, please!");

			Transport transport = session.getTransport("smtp");
                        
			transport.connect(host,port,username,password);

			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
