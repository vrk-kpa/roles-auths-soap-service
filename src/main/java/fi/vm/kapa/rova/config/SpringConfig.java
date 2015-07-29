package fi.vm.kapa.rova.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ImportResource("applicationContext.xml")
@PropertySources(value =  {@PropertySource("classpath:application.properties"),
		@PropertySource(value="classpath:developer.properties", ignoreResourceNotFound=true), 
		@PropertySource(value="file:/opt/www/roles-auths-soap-service/config/service.properties", ignoreResourceNotFound=true)})
public class SpringConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

}

