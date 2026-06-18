package br.com.adaneinstein.wheresmymoney.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:version.properties")
public class AppVersion {

    private static String current = "dev";

    @Value("${app.version:dev}")
    public void setCurrent(String v) { current = v; }

    public static String get() { return current; }
}
