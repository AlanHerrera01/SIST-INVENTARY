package com.veris.inventory.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades tipadas de configuración transversal. */
@Configuration
@EnableConfigurationProperties({IdentityProperties.class, SecurityProperties.class})
public class PropertiesConfiguration {
}
