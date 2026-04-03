package ro.axonsoft.eval.minibank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.axonsoft.eval.minibank.dto.AccountResponse;
import ro.axonsoft.eval.minibank.dto.CreateAccountRequest;
import ro.axonsoft.eval.minibank.dto.PageResponse;
import ro.axonsoft.eval.minibank.dto.TransactionResponse;
import ro.axonsoft.eval.minibank.exception.ConflictException;
import ro.axonsoft.eval.minibank.exception.NotFoundException;
import ro.axonsoft.eval.minibank.exception.ValidationException;
import ro.axonsoft.eval.minibank.model.Account;
import ro.axonsoft.eval.minibank.repository.AccountRepository;
import ro.axonsoft.eval.minibank.repository.TransactionRepository;
import ro.axonsoft.eval.minibank.util.IbanUtil;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        // Validate IBAN format
        if (!IbanUtil.isValidIban(request.getIban())) {
            throw new ValidationException("Invalid IBAN: " + request.getIban());
        }

        // Check IBAN uniqueness
        if (accountRepository.existsByIban(request.getIban())) {
            throw new ConflictException("IBAN already in use: " + request.getIban());
        }

        Account account = Account.builder()
                .ownerName(request.getOwnerName())
                .iban(request.getIban())
                .currency(request.getCurrency())
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO.setScale(2))
                .createdAt(Instant.now())
                .build();

        account = accountRepository.save(account);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> listAccounts(int page, int size) {
        Page<AccountResponse> result = accountRepository
                .findAll(PageRequest.of(page, size))
                .map(this::toResponse);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(Long accountId, int page, int size) {
        // Ensure account exists
        if (!accountRepository.existsById(accountId)) {
            throw new NotFoundException("Account not found: " + accountId);
        }
        Page<TransactionResponse> result = transactionRepository
                .findByAccountIdOrderByTimestampAsc(accountId, PageRequest.of(page, size))
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .timestamp(t.getTimestamp())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .currency(t.getCurrency())
                        .balanceAfter(t.getBalanceAfter())
                        .counterpartyIban(t.getCounterpartyIban())
                        .transferId(t.getTransferId())
                        .build());
        return PageResponse.of(result);
    }

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .ownerName(account.getOwnerName())
                .iban(account.getIban())
                .currency(account.getCurrency())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
