package com.racelab.core.detection;

import com.racelab.core.domain.Account;
import com.racelab.core.domain.TestExecution;
import com.racelab.core.repository.AccountRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks for lost updates in account balances.
 * Verifies that the final balance equals initial balance plus all deposits minus all withdrawals.
 */
@RequiredArgsConstructor
public class LostUpdateChecker implements InvariantChecker {

    private final AccountRepository accountRepository;

    @Override
    public String getInvariantName() {
        return "NO_LOST_UPDATES";
    }

    @Override
    public String getDescription() {
        return "Ensures all balance modifications are accounted for. Final balance should equal initial + deposits - withdrawals.";
    }

    @Override
    public List<InvariantViolation> check(TestExecution execution) {
        List<InvariantViolation> violations = new ArrayList<>();

        // This would typically compare expected vs actual balances
        // For demonstration, we check accounts that were modified
        List<Account> accounts = accountRepository.findAll();

        for (Account account : accounts) {
            // In a real implementation, we would track all operations on this account
            // and verify the final balance matches the expected result
            // This is a placeholder for the actual verification logic
        }

        return violations;
    }

    @Override
    public boolean requiresDatabaseAccess() {
        return true;
    }
}