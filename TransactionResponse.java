package ro.axonsoft.eval.minibank.dto;

import lombok.Builder;
import lombok.Data;
import ro.axonsoft.eval.minibank.model.Account;
import ro.axonsoft.eval.minibank.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Instant timestamp;
    private Transaction.TransactionType type;
    private BigDecimal amount;
    private Account.Currency currency;
    private BigDecimal balanceAfter;
    private String counterpartyIban;
    private Long transferId;
}
