package fi.vm.kapa.rova.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.request.RequestContextListener;

import com.sun.xml.ws.transport.http.servlet.WSSpringServlet;

@Configuration
@ImportResource("applicationContext.xml")
public class Initializer extends SpringBootServletInitializer implements WebApplicationInitializer {

	@Override
	public void onStartup(ServletContext ctx) throws ServletException {
		ctx.addListener(RequestContextListener.class);
	}
	
	@Bean
	public WSSpringServlet wsSpringServlet() {
		return new WSSpringServlet();
	}
}