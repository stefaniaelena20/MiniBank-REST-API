package ro.axonsoft.eval.minibank.dto;

import lombok.Builder;
import lombok.Data;
import ro.axonsoft.eval.minibank.model.Account;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransferResponse {
    private Long id;
    private String sourceIban;
    private String targetIban;
    private BigDecimal amount;
    private Account.Currency currency;
    private Account.Currency targetCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private String idempotencyKey;
    private Instant createdAt;
}
