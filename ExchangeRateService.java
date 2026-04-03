package ro.axonsoft.eval.minibank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.axonsoft.eval.minibank.config.ExchangeRateConfig;
import ro.axonsoft.eval.minibank.model.Account;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateConfig exchangeRateConfig;

    public Map<String, BigDecimal> getAllRates() {
        return exchangeRateConfig.getRates();
    }

    public BigDecimal getRate(String currency) {
        return exchangeRateConfig.getRate(currency);
    }

    public BigDecimal getRate(Account.Currency currency) {
        return exchangeRateConfig.getRate(currency.name());
    }

    /**
     * Convert amount from sourceCurrency to targetCurrency.
     * Formula: convertedAmount = amount * (sourceToRON / targetToRON)
     */
    public BigDecimal convert(BigDecimal amount, Account.Currency source, Account.Currency target) {
        if (source == target) {
            return amount.setScale(2, RoundingMode.HALF_EVEN);
        }
        BigDecimal sourceRate = getRate(source);
        BigDecimal targetRate = getRate(target);
        // Use higher precision for intermediate calculation
        return amount.multiply(sourceRate)
                .divide(targetRate, 2, RoundingMode.HALF_EVEN);
    }

    /**
     * Returns the effective exchange rate: sourceToRON / targetToRON, scale 6
     */
    public BigDecimal getEffectiveRate(Account.Currency source, Account.Currency target) {
        BigDecimal sourceRate = getRate(source);
        BigDecimal targetRate = getRate(target);
        return sourceRate.divide(targetRate, 6, RoundingMode.HALF_EVEN);
    }

    /**
     * Convert any currency amount to EUR equivalent (for SAVINGS limit checking).
     */
    public BigDecimal toEurEquivalent(BigDecimal amount, Account.Currency currency) {
        BigDecimal currencyRate = getRate(currency);    // RON per 1 unit of currency
        BigDecimal eurRate = getRate(Account.Currency.EUR); // RON per 1 EUR
        // EUR equivalent = amount * (currencyToRON / eurToRON)
        return amount.multiply(currencyRate)
                .divide(eurRate, 2, RoundingMode.HALF_EVEN);
    }
}
