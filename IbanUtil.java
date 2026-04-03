package ro.axonsoft.eval.minibank.util;

import org.apache.commons.validator.routines.IBANValidator;

import java.util.Set;

public class IbanUtil {

    private static final IBANValidator IBAN_VALIDATOR = IBANValidator.getInstance();

    // SEPA member countries (ISO 3166-1 alpha-2 country codes)
    private static final Set<String> SEPA_COUNTRIES = Set.of(
        "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
        "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
        "PL", "PT", "RO", "SK", "SI", "ES", "SE",
        // Non-EU SEPA members
        "IS", "LI", "NO", "CH", "GB", "MC", "SM", "VA", "AD", "GI",
        "IM", "JE", "GG"
    );

    public static boolean isValidIban(String iban) {
        if (iban == null || iban.isBlank()) return false;
        return IBAN_VALIDATOR.isValid(iban);
    }

    public static boolean isSepaIban(String iban) {
        if (iban == null || iban.length() < 2) return false;
        String countryCode = iban.substring(0, 2).toUpperCase();
        return SEPA_COUNTRIES.contains(countryCode);
    }
}
