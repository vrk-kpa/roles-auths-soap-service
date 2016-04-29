package fi.vm.kapa.rova.config;

import java.lang.reflect.Field;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;
import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;

import fi.vm.kapa.rova.logging.LogbackConfigurator;
import fi.vm.kapa.rova.logging.MDCFilter;

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
                .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), false, "/*");
    }

    @Bean
    public WSSpringServlet wsSpringServlet() {
        return new WSSpringServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            public void destroy() {
                // Problem: WSSpringServlet tries to destroy the WebApplicationContext on its own,
                // in addition to the usual shutdown from ContextLoaderListener. This causes a deadlock
                // preventing the JVM from shutting down.
                //
                // Solution: A custom destroy()-implementation that doesn't try to destroy the context. Unfortunately
                // the internal 'delegate' reference is private, so we need some reflection wizardry to access it. 
                try {
                    Field f = WSSpringServlet.class.getDeclaredField("delegate");
                    f.setAccessible(true);
                    WSServletDelegate delegate = (WSServletDelegate)f.get(this);
                    delegate.destroy();
                } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
