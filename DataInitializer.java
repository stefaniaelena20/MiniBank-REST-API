package ro.axonsoft.eval.minibank.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ro.axonsoft.eval.minibank.model.Account;
import ro.axonsoft.eval.minibank.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String BANK_IBAN = "RO49AAAA1B31007593840000";

    private final AccountRepository accountRepository;

    @Override
    public void run(ApplicationArguments args) {
        // Only seed if not already present
        if (!accountRepository.existsByIban(BANK_IBAN)) {
            Account bank = Account.builder()
                    .ownerName("MiniBank System")
                    .iban(BANK_IBAN)
                    .currency(Account.Currency.RON)
                    .accountType(Account.AccountType.CHECKING)
                    .balance(BigDecimal.ZERO)
                    .createdAt(Instant.now())
                    .build();
            accountRepository.save(bank);
            // Should produce ID=1 since it's the first insert
        }
    }
}
