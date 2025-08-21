package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class CfmsGeneralesAlmacenamientoObjetosApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfmsGeneralesAlmacenamientoObjetosApiApplication.class, args);
	}

}
