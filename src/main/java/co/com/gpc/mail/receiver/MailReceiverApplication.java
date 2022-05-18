package co.com.gpc.mail.receiver;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * Mail class Spring Boot Mail Receiver
 * @author Sammy
 */
@SpringBootApplication
@EnableEncryptableProperties
public class MailReceiverApplication {

    
	public static void main(String[] args) {
		SpringApplication.run(MailReceiverApplication.class, args);
	}

}
