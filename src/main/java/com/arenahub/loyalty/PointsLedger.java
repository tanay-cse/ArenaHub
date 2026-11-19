package com.arenahub.loyalty;

import com.arenahub.auth.User;
import com.arenahub.booking.Booking;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "points_ledger")
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id")
    private Reward reward;

    @Column(name = "points_change", nullable = false)
    private Integer pointsChange;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PointsLedger() {
    }

    public PointsLedger(User user, Integer pointsChange, String transactionType, String description) {
        this.user = user;
        this.pointsChange = pointsChange;
        this.transactionType = transactionType;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public PointsLedger(User user, Booking booking, Reward reward, Integer pointsChange, String transactionType, String description) {
        this.user = user;
        this.booking = booking;
        this.reward = reward;
        this.pointsChange = pointsChange;
        this.transactionType = transactionType;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Reward getReward() {
        return reward;
    }

    public void setReward(Reward reward) {
        this.reward = reward;
    }

    public Integer getPointsChange() {
        return pointsChange;
    }

    public void setPointsChange(Integer pointsChange) {
        this.pointsChange = pointsChange;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
