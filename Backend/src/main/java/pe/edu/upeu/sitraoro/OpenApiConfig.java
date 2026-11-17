package pe.edu.upeu.sitraoro;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI sitraOroOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SITRA-ORO API - Modulo Acopiador")
                .version("v1")
                .description("Contrato REST del modulo de acopio de oro"));
    }
}
