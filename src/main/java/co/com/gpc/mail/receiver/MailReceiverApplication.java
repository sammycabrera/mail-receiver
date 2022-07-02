package co.com.gpc.mail.receiver;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
/**
 * Mail class Spring Boot Mail Receiver
 * @author Sammy
 */
@SpringBootApplication
//@PropertySources({
         //@PropertySource(value = "file:config/application.yml")})
public class MailReceiverApplication {

    
	public static void main(String[] args) {
		SpringApplication.run(MailReceiverApplication.class, args);
	}

}
