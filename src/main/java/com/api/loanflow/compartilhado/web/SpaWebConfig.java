package com.api.loanflow.compartilhado.web;

import com.api.loanflow.compartilhado.apresentacao.FrontendProjectPaths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String distLocation = ensureTrailingSlash(FrontendProjectPaths.resolveDistDirectory().toUri().toString());
		String assetsLocation = ensureTrailingSlash(
			FrontendProjectPaths.resolveDistDirectory().resolve("assets").toUri().toString()
		);

		registry.addResourceHandler("/index.html", "/favicon.png")
			.addResourceLocations(distLocation);

		registry.addResourceHandler("/assets/**")
			.addResourceLocations(assetsLocation);
	}

	private static String ensureTrailingSlash(String location) {
		return location.endsWith("/") ? location : location + "/";
	}
}
