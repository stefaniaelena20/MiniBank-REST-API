package ro.axonsoft.eval.minibank.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sourceIban;

    @Column(nullable = false)
    private String targetIban;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Account.Currency currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Account.Currency targetCurrency;

    // exchangeRate = sourceToRON / targetToRON, scale 6
    @Column(precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    // amount in target currency, scale 2
    @Column(precision = 19, scale = 2)
    private BigDecimal convertedAmount;

    @Column(unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt;
}
