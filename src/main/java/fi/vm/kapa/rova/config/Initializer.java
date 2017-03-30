/**
 * The MIT License
 * Copyright (c) 2016 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.rova.config;

import com.sun.xml.ws.transport.http.servlet.WSServletDelegate;
import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;
import fi.vm.kapa.rova.logging.LogbackConfigurator;
import fi.vm.kapa.rova.logging.MDCFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.lang.reflect.Field;
import java.util.EnumSet;

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
