package fi.vm.kapa.rova.config;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import fi.vm.kapa.rova.logging.LogbackConfigurator;
import fi.vm.kapa.rova.logging.MDCFilter;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;

@Configuration
@ImportResource("applicationContext.xml")
public class Initializer extends SpringBootServletInitializer {

    private static final String MDC_FILTER = "mdcFilter";

    @Autowired
    private LogbackConfigurator logConfigurator;

    @Bean
    public EmbeddedServletContainerCustomizer embeddedServletContainerCustomizer() {
        return logConfigurator.containerCustomizer();
    }

    @Override
    public void onStartup(ServletContext ctx) throws ServletException {
        super.onStartup(ctx);
        ctx.addFilter(MDC_FILTER, MDCFilter.class)
                .addMappingForUrlPatterns(
                        EnumSet.of(DispatcherType.REQUEST,
                                DispatcherType.FORWARD), false, "/*");
    }


    @Bean
    public WSSpringServlet wsSpringServlet() {
        return new WSSpringServlet();
    }
}
