package ro.axonsoft.eval.minibank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ro.axonsoft.eval.minibank.model.Account;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "ownerName is required")
    private String ownerName;

    @NotBlank(message = "iban is required")
    private String iban;

    @NotNull(message = "currency is required")
    private Account.Currency currency;

    @NotNull(message = "accountType is required")
    private Account.AccountType accountType;
}
