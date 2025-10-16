package com.medosasoftware.mdstracking;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MdsTrackingApplication {

    public static void main(String[] args) {
        // 🌱 .env dosyasını yükle
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        // 🔁 Tüm .env değişkenlerini System property olarak aktar
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        // 🧩 Debug için bir satır (görmek istersen)
        System.out.println("✅ .env yüklendi. APP_PORT = " + System.getProperty("APP_PORT"));

        // 🚀 Spring Boot'u başlat
        SpringApplication.run(MdsTrackingApplication.class, args);
    }
}
