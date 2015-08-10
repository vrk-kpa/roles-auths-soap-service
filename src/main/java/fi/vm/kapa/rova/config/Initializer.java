package fi.vm.kapa.rova.config;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("applicationContext.xml")
public class Initializer extends SpringBootServletInitializer {

    @Bean
    public WSSpringServlet wsSpringServlet() {
        return new WSSpringServlet();
    }
}
