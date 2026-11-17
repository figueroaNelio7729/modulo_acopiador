package pe.edu.upeu.sitraoro;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Permite que el frontend Angular (4200 / 4201) consuma la API desde el navegador. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // /actuator tiene su propio CORS: management.endpoints.web.cors en application-dev.yml
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://localhost:4201")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
