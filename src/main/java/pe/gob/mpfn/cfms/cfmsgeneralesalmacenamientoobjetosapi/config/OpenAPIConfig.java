package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI usersMicroserviceOpenAPI() {

        Contact contact = new Contact();
        contact.setEmail("dacunac@email.mpfn.gob.pe");
        contact.setName("OGTI");
        contact.setUrl("http://www.mpfn.gob.pe");

        return new OpenAPI()
                .info(new Info()
                        .title("Cfms Generales Almacenamiento Objetos Api")
                        .description("Servicio que permite el almacenamiento de objetos")
                        .version("1.0")
                        .contact(contact)
                );
    }
}
