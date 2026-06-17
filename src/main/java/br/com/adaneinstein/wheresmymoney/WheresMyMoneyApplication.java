package br.com.adaneinstein.wheresmymoney;

import br.com.adaneinstein.wheresmymoney.tui.MainScreen;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
public class WheresMyMoneyApplication {

    private final MainScreen mainScreen;

    static void main(String[] args) {
        SpringApplication.run(WheresMyMoneyApplication.class, args);
    }

    @Bean
    public CommandLineRunner tuiRunner(){
        return _ -> {
            mainScreen.start();
        };
    }

}
