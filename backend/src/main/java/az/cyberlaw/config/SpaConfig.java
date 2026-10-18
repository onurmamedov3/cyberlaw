package az.cyberlaw.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React app (frontend → src/main/resources/static) and sends unknown,
 * non-API paths to index.html so that client-side routes like /chat survive a page refresh.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new PathResourceResolver() {
				@Override
				protected Resource getResource(String resourcePath, Resource location) throws IOException {
					Resource requested = location.createRelative(resourcePath);
					if (requested.exists() && requested.isReadable()) {
						return requested;
					}
					if (resourcePath.startsWith("api/")) {
						return null;
					}
					Resource index = new ClassPathResource("/static/index.html");
					return index.exists() ? index : null;
				}
			});
	}

}
