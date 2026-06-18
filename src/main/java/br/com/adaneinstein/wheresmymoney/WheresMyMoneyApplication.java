package br.com.adaneinstein.wheresmymoney;

import br.com.adaneinstein.wheresmymoney.tui.MainScreen;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportRuntimeHints(HibernateNativeHints.class)
@RequiredArgsConstructor
public class WheresMyMoneyApplication {

    private final MainScreen mainScreen;

    public static void main(String[] args) {
        SpringApplication.run(WheresMyMoneyApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public CommandLineRunner tuiRunner(){
        return _ -> {
            mainScreen.start();
        };
    }

}
