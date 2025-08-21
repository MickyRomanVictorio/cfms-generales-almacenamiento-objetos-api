package pe.gob.mpfn.cfms.cfmsgeneralesalmacenamientoobjetosapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AlfrescoConfig {


    @Value("${alfresco.username}")
    private String username;

    @Value("${alfresco.password}")
    private String password;


    @Value("${alfresco.base-url}")
    private String urlBase;

    @Value("${alfresco.share-url}")
    private String urlShare;

    @Value("${webclient.buffer.max-in-memory-size}")
    private String maxInMemorySize;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrlBase() {
        return urlBase;
    }
    public String getUrlShare() {
        return urlShare;
    }


    public String getmaxInMemorySize() {
        return maxInMemorySize;
    }

}
