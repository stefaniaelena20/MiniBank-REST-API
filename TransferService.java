package ro.axonsoft.eval.minibank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.axonsoft.eval.minibank.dto.CreateTransferRequest;
import ro.axonsoft.eval.minibank.dto.PageResponse;
import ro.axonsoft.eval.minibank.dto.TransferResponse;
import ro.axonsoft.eval.minibank.exception.NotFoundException;
import ro.axonsoft.eval.minibank.exception.ValidationException;
import ro.axonsoft.eval.minibank.model.Account;
import ro.axonsoft.eval.minibank.model.Transaction;
import ro.axonsoft.eval.minibank.model.Transfer;
import ro.axonsoft.eval.minibank.repository.AccountRepository;
import ro.axonsoft.eval.minibank.repository.TransactionRepository;
import ro.axonsoft.eval.minibank.repository.TransferRepository;
import ro.axonsoft.eval.minibank.util.IbanUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final Long BANK_ACCOUNT_ID = 1L;

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;

    @Transactional
    public TransferResponse createTransfer(CreateTransferRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return transferRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::toResponse)
                    .orElseGet(() -> doCreateTransfer(request));
        }
        return doCreateTransfer(request);
    }

    private TransferResponse doCreateTransfer(CreateTransferRequest request) {
        String sourceIban = request.getSourceIban();
        String targetIban = request.getTargetIban();
        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_EVEN);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive");
        }

        if (!IbanUtil.isSepaIban(sourceIban)) {
            throw new ValidationException("Source IBAN is not from a SEPA country: " + sourceIban);
        }
        if (!IbanUtil.isSepaIban(targetIban)) {
            throw new ValidationException("Target IBAN is not from a SEPA country: " + targetIban);
        }

        if (sourceIban.equals(targetIban)) {
            throw new ValidationException("Source and target IBANs must be different");
        }

        Account sourceAccount = accountRepository.findByIban(sourceIban)
                .orElseThrow(() -> new NotFoundException("Source account not found: " + sourceIban));
        Account targetAccount = accountRepository.findByIban(targetIban)
                .orElseThrow(() -> new NotFoundException("Target account not found: " + targetIban));

        // Pessimistic locks in ID order to avoid deadlock
        final Long lowerId;
        final Long higherId;
        if (sourceAccount.getId() < targetAccount.getId()) {
            lowerId = sourceAccount.getId();
            higherId = targetAccount.getId();
        } else {
            lowerId = targetAccount.getId();
            higherId = sourceAccount.getId();
        }

        Account lockedLower = accountRepository.findByIdWithLock(lowerId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + lowerId));
        Account lockedHigher = accountRepository.findByIdWithLock(higherId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + higherId));

        // Re-assign locked versions
        if (sourceAccount.getId().equals(lockedLower.getId())) {
            sourceAccount = lockedLower;
            targetAccount = lockedHigher;
        } else {
            sourceAccount = lockedHigher;
            targetAccount = lockedLower;
        }

        boolean sourceIsBank = sourceAccount.getId().equals(BANK_ACCOUNT_ID);
        boolean targetIsBank = targetAccount.getId().equals(BANK_ACCOUNT_ID);

        if (!sourceIsBank) {
            if (sourceAccount.getBalance().compareTo(amount) < 0) {
                throw new ValidationException("Insufficient funds in source account");
            }
            if (sourceAccount.getAccountType() == Account.AccountType.SAVINGS) {
                checkSavingsDailyLimit(sourceAccount, amount);
            }
        }

        Account.Currency sourceCurrency = sourceAccount.getCurrency();
        Account.Currency targetCurrency = targetAccount.getCurrency();
        BigDecimal convertedAmount;
        BigDecimal effectiveRate;

        if (sourceCurrency == targetCurrency) {
            convertedAmount = amount;
            effectiveRate = null;
        } else {
            effectiveRate = exchangeRateService.getEffectiveRate(sourceCurrency, targetCurrency);
            convertedAmount = exchangeRateService.convert(amount, sourceCurrency, targetCurrency);
        }

        Instant now = Instant.now();

        Transfer transfer = Transfer.builder()
                .sourceIban(sourceIban)
                .targetIban(targetIban)
                .amount(amount)
                .currency(sourceCurrency)
                .targetCurrency(targetCurrency)
                .exchangeRate(effectiveRate)
                .convertedAmount(sourceCurrency == targetCurrency ? null : convertedAmount)
                .idempotencyKey(request.getIdempotencyKey())
                .createdAt(now)
                .build();
        transfer = transferRepository.save(transfer);

        if (!sourceIsBank) {
            BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(amount);
            sourceAccount.setBalance(newSourceBalance);
            accountRepository.save(sourceAccount);
        }

        if (!targetIsBank) {
            BigDecimal newTargetBalance = targetAccount.getBalance().add(convertedAmount);
            targetAccount.setBalance(newTargetBalance);
            accountRepository.save(targetAccount);
        }

        if (sourceIsBank) {
            Transaction depositTx = Transaction.builder()
                    .accountId(targetAccount.getId())
                    .timestamp(now)
                    .type(Transaction.TransactionType.DEPOSIT)
                    .amount(convertedAmount)
                    .currency(targetCurrency)
                    .balanceAfter(targetAccount.getBalance())
                    .counterpartyIban(null)
                    .transferId(transfer.getId())
                    .build();
            transactionRepository.save(depositTx);

        } else if (targetIsBank) {
            Transaction withdrawalTx = Transaction.builder()
                    .accountId(sourceAccount.getId())
                    .timestamp(now)
                    .type(Transaction.TransactionType.WITHDRAWAL)
                    .amount(amount)
                    .currency(sourceCurrency)
                    .balanceAfter(sourceAccount.getBalance())
                    .counterpartyIban(null)
                    .transferId(transfer.getId())
                    .build();
            transactionRepository.save(withdrawalTx);

        } else {
            Transaction outTx = Transaction.builder()
                    .accountId(sourceAccount.getId())
                    .timestamp(now)
                    .type(Transaction.TransactionType.TRANSFER_OUT)
                    .amount(amount)
                    .currency(sourceCurrency)
                    .balanceAfter(sourceAccount.getBalance())
                    .counterpartyIban(targetIban)
                    .transferId(transfer.getId())
                    .build();
            transactionRepository.save(outTx);

            Transaction inTx = Transaction.builder()
                    .accountId(targetAccount.getId())
                    .timestamp(now)
                    .type(Transaction.TransactionType.TRANSFER_IN)
                    .amount(convertedAmount)
                    .currency(targetCurrency)
                    .balanceAfter(targetAccount.getBalance())
                    .counterpartyIban(sourceIban)
                    .transferId(transfer.getId())
                    .build();
            transactionRepository.save(inTx);
        }

        return toResponse(transfer);
    }

    private void checkSavingsDailyLimit(Account sourceAccount, BigDecimal amount) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        Instant dayStart = now.truncatedTo(ChronoUnit.DAYS).toInstant();
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        List<Transfer> todayTransfers = transferRepository
                .findBySourceIbanAndCreatedAtBetween(sourceAccount.getIban(), dayStart, dayEnd);

        BigDecimal cumulativeEur = BigDecimal.ZERO;
        for (Transfer t : todayTransfers) {
            BigDecimal eurEquiv = exchangeRateService.toEurEquivalent(t.getAmount(), t.getCurrency());
            cumulativeEur = cumulativeEur.add(eurEquiv);
        }

        BigDecimal currentEurEquiv = exchangeRateService.toEurEquivalent(amount, sourceAccount.getCurrency());
        BigDecimal newTotal = cumulativeEur.add(currentEurEquiv);

        BigDecimal limit = new BigDecimal("5000.00");
        if (newTotal.compareTo(limit) > 0) {
            throw new ValidationException(
                    "SAVINGS account daily outgoing limit of 5000 EUR equivalent exceeded. " +
                    "Current total would be: " + newTotal + " EUR");
        }
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(Long id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found: " + id));
        return toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> listTransfers(String iban, Instant fromDate, Instant toDate,
                                                         int page, int size) {
        Page<TransferResponse> result = transferRepository
                .findWithFilters(iban, fromDate, toDate, PageRequest.of(page, size))
                .map(this::toResponse);
        return PageResponse.of(result);
    }

    public TransferResponse toResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .sourceIban(transfer.getSourceIban())
                .targetIban(transfer.getTargetIban())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .targetCurrency(transfer.getTargetCurrency())
                .exchangeRate(transfer.getExchangeRate())
                .convertedAmount(transfer.getConvertedAmount())
                .idempotencyKey(transfer.getIdempotencyKey())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
