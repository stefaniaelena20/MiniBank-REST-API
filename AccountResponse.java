package ro.axonsoft.eval.minibank.dto;

import lombok.Data;
import lombok.Builder;
import ro.axonsoft.eval.minibank.model.Account;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AccountResponse {
    private Long id;
    private String ownerName;
    private String iban;
    private Account.Currency currency;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private Instant createdAt;
}
