package com.arenahub.booking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_rules")
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "station_type", length = 30)
    private String stationType;

    @Column(name = "start_hour", nullable = false)
    private Byte startHour;

    @Column(name = "end_hour", nullable = false)
    private Byte endHour;

    @Column(name = "rate_type", nullable = false, length = 20)
    private String rateType;

    @Column(name = "multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal multiplier;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PricingRule() {
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getStationType() {
        return stationType;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public Byte getStartHour() {
        return startHour;
    }

    public void setStartHour(Byte startHour) {
        this.startHour = startHour;
    }

    public void setStartHour(Integer startHour) {
        this.startHour = startHour != null ? startHour.byteValue() : null;
    }

    public Byte getEndHour() {
        return endHour;
    }

    public void setEndHour(Byte endHour) {
        this.endHour = endHour;
    }

    public void setEndHour(Integer endHour) {
        this.endHour = endHour != null ? endHour.byteValue() : null;
    }

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
