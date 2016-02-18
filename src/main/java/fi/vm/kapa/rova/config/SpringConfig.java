package fi.vm.kapa.rova.config;

import fi.vm.kapa.rova.engine.evaluation.BaseSpringConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ImportResource("applicationContext.xml")
@PropertySources(value = {
    @PropertySource("classpath:application.properties"),
    @PropertySource(value = "file:/opt/rova/roles-auths-soap-service/config/service.properties", ignoreResourceNotFound = true)})
public class SpringConfig extends BaseSpringConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
