package ro.axonsoft.eval.minibank.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.axonsoft.eval.minibank.model.Transfer;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transfer t WHERE " +
           "(:iban IS NULL OR t.sourceIban = :iban OR t.targetIban = :iban) AND " +
           "(:fromDate IS NULL OR t.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<Transfer> findWithFilters(
            @Param("iban") String iban,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);

    // For SAVINGS daily limit: sum outgoing from sourceIban on a calendar day
    @Query("SELECT t FROM Transfer t WHERE t.sourceIban = :iban AND t.createdAt >= :dayStart AND t.createdAt < :dayEnd")
    java.util.List<Transfer> findBySourceIbanAndCreatedAtBetween(
            @Param("iban") String iban,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd);
}
