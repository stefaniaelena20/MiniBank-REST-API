package ro.axonsoft.eval.minibank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "exchange")
@EnableConfigurationProperties
public class ExchangeRateConfig {

    private Map<String, BigDecimal> rates;

    public Map<String, BigDecimal> getRates() {
        return rates;
    }

    public void setRates(Map<String, BigDecimal> rates) {
        this.rates = rates;
    }

    public BigDecimal getRate(String currency) {
        if (rates == null) {
            throw new IllegalStateException("Exchange rates not loaded");
        }
        BigDecimal rate = rates.get(currency);
        if (rate == null) {
            throw new IllegalArgumentException("No exchange rate for currency: " + currency);
        }
        return rate;
    }
}
