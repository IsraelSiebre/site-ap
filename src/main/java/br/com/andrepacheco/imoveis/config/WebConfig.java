package br.com.andrepacheco.imoveis.config;

import br.com.andrepacheco.imoveis.service.UploadService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UploadService uploads;
    public WebConfig(UploadService uploads) { this.uploads = uploads; }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploads.getDirectory().toUri().toString())
                .setCachePeriod(3600);
    }
}
