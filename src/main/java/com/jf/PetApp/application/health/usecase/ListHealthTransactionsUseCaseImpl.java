package com.jf.PetApp.application.health.usecase;

import static com.jf.PetApp.core.domain.health.HealthModels.*;
import static com.jf.PetApp.application.health.dto.HealthCommands.*;
import static com.jf.PetApp.application.health.service.HealthCalculations.*;
import static com.jf.PetApp.application.health.service.HealthValidation.*;

import com.jf.PetApp.application.common.exception.ResourceNotFoundException;
import com.jf.PetApp.application.health.exception.HealthConflictException;
import com.jf.PetApp.application.health.port.HealthStore;
import com.jf.PetApp.application.health.service.HealthLookups;
import com.jf.PetApp.application.health.service.InvoiceCycleResolver;
import com.jf.PetApp.application.health.service.RecurrenceMaterializer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
public class ListHealthTransactionsUseCaseImpl implements ListHealthTransactionsUseCase {

    /**
     * Bounds what used to be an unbounded {@code SELECT *}. The listing endpoint still returns a
     * plain JSON array (see {@code HealthTransactionController}) rather than a paginated envelope
     * -- the mobile client neither sends nor expects page/size params today (it decodes a bare
     * list) -- so introducing a page contract here would be a breaking API change made without a
     * mobile consumer ready for it. This cap is the conservative half of the fix: move filtering
     * into SQL and stop scanning/returning every row a user has ever recorded, without changing
     * the response shape. A real pagination contract is a separate, larger change.
     */
    private static final int MAX_RESULTS = 500;

    private final HealthStore store;
    private final HealthLookups lookups;
    private final RecurrenceMaterializer recurrences;

    public ListHealthTransactionsUseCaseImpl(HealthStore store, HealthLookups lookups, RecurrenceMaterializer recurrences) {
        this.store = store;
        this.lookups = lookups;
        this.recurrences = recurrences;
    }

    @Override
    @Transactional
    public List<Transaction> execute(String email, TransactionFilter filter) {
        long userId = lookups.userId(email);
        lookups.requireProfile(userId);
        if (filter != null && filter.to() != null) {
            recurrences.materializeRecurrences(userId, YearMonth.from(filter.to()));
        } else {
            recurrences.materializeRecurrences(userId, YearMonth.now());
        }
        if (filter == null) {
            return store.listTransactions(userId, null, null, null, null, null, MAX_RESULTS);
        }
        EntryStatus wantedStatus = filter.status() == null || filter.status().isBlank()
                ? null : enumValue(EntryStatus.class, filter.status(), "status");
        String category = filter.category() == null || filter.category().isBlank() ? null : filter.category();
        return store.listTransactions(userId, filter.from(), filter.to(), filter.accountId(), category, wantedStatus, MAX_RESULTS);
    }
}
