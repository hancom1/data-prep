package org.talend.dataprep.preparation.configuration;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;

@Configuration("org.talend.dataprep.preparation.configuration")
@EnableSwagger
public class Documentation {

    private SpringSwaggerConfig springSwaggerConfig;

    @Autowired
    public void setSpringSwaggerConfig(SpringSwaggerConfig springSwaggerConfig) {
        this.springSwaggerConfig = springSwaggerConfig;
    }

    @Bean
    public SwaggerSpringMvcPlugin customImplementation() {
        ApiInfo apiInfo = new ApiInfo("Talend Data Preparation - Preparation Service (PS)",
                "This service exposes operations on preparations (creation, remove...).", StringUtils.EMPTY, StringUtils.EMPTY,
                StringUtils.EMPTY, StringUtils.EMPTY);
        return new SwaggerSpringMvcPlugin(springSwaggerConfig).apiInfo(apiInfo).includePatterns(".*preparations.*"); //$NON-NLS-1
    }
}