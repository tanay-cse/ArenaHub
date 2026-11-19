package com.arenahub.loyalty;

import com.arenahub.auth.User;
import com.arenahub.auth.UserRepository;
import com.arenahub.booking.Booking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class LoyaltyService {

    private static final Set<String> VALID_TRANSACTION_TYPES = Set.of(
            "EARNED_BOOKING",
            "REWARD_REDEMPTION",
            "ADJUSTMENT"
    );

    private final PointsLedgerRepository pointsLedgerRepository;
    private final UserRepository userRepository;

    public LoyaltyService(PointsLedgerRepository pointsLedgerRepository, UserRepository userRepository) {
        this.pointsLedgerRepository = pointsLedgerRepository;
        this.userRepository = userRepository;
    }

    public void addPoints(Long userId, int pointsChange, String transactionType, String description) {
        addPoints(userId, pointsChange, transactionType, description, null);
    }

    public void addPoints(Long userId, int pointsChange, String transactionType, String description, Booking booking) {
        if (pointsChange == 0) {
            throw new IllegalArgumentException("Points change cannot be zero.");
        }
        if (transactionType == null || !VALID_TRANSACTION_TYPES.contains(transactionType)) {
            throw new IllegalArgumentException("Invalid transaction type: " + transactionType);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        PointsLedger entry = new PointsLedger(user, pointsChange, transactionType, description);
        if (booking != null) {
            entry.setBooking(booking);
        }
        pointsLedgerRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public int getBalance(Long userId) {
        Long balance = pointsLedgerRepository.calculateBalanceByUserId(userId);
        return balance != null ? balance.intValue() : 0;
    }

    @Transactional(readOnly = true)
    List<PointsLedger> getHistory(Long userId) {
        return pointsLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
