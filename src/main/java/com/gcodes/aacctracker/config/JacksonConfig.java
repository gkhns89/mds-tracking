package com.gcodes.aacctracker.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Configuration for Hibernate Lazy Loading & Java Time Support
 * <p>
 * Bu yapılandırma Hibernate proxy hatalarını önler
 * ve Spring Boot'un varsayılan Jackson modüllerini (örneğin JavaTimeModule)
 * koruyarak JSON serileştirmesini optimize eder.
 */
@Configuration
public class JacksonConfig {

    /**
     * Hibernate modülünü Jackson yapılandırmasına ekler.
     * Lazy-loaded entity'leri kontrol altında serialize eder.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            Hibernate6Module hibernateModule = new Hibernate6Module();

            // Lazy-loaded field'ları otomatik yükleme (false)
            hibernateModule.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);

            // Proxy'ler için sadece identifier serialize et
            hibernateModule.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);

            // Hibernate6Module'u mevcut modüllere ekle
            builder.modulesToInstall(hibernateModule);
        };
    }
}
