package com.arenahub.booking;

import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class PricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final StationRepository stationRepository;

    public PricingService(PricingRuleRepository pricingRuleRepository,
                          StationRepository stationRepository) {
        this.pricingRuleRepository = pricingRuleRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(Long stationId, int startHour) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));

        BigDecimal hourlyRate = station.getHourlyRate();
        List<PricingRule> activeRules = pricingRuleRepository.findByIsActiveTrue();

        // Find active PEAK or OFF_PEAK multiplier whose [start_hour, end_hour) contains startHour
        // and whose station_type is NULL or matches the station (if none applies, use 1.0)
        Optional<PricingRule> matchedPeakOffPeak = activeRules.stream()
                .filter(r -> "PEAK".equalsIgnoreCase(r.getRateType()) || "OFF_PEAK".equalsIgnoreCase(r.getRateType()))
                .filter(r -> r.getStartHour() <= startHour && startHour < r.getEndHour())
                .filter(r -> r.getStationType() == null || r.getStationType().isBlank()
                        || r.getStationType().equalsIgnoreCase(station.getStationType()))
                .sorted((r1, r2) -> {
                    boolean r1Specific = r1.getStationType() != null && !r1.getStationType().isBlank();
                    boolean r2Specific = r2.getStationType() != null && !r2.getStationType().isBlank();
                    if (r1Specific != r2Specific) {
                        return r1Specific ? -1 : 1;
                    }
                    return r2.getId().compareTo(r1.getId());
                })
                .findFirst();

        BigDecimal peakOffPeakMultiplier = matchedPeakOffPeak
                .map(PricingRule::getMultiplier)
                .orElse(BigDecimal.valueOf(1.0));

        // Find active MEMBER multiplier (always applied, since every logged-in user is a member)
        Optional<PricingRule> matchedMember = activeRules.stream()
                .filter(r -> "MEMBER".equalsIgnoreCase(r.getRateType()))
                .filter(r -> r.getStartHour() <= startHour && startHour < r.getEndHour())
                .filter(r -> r.getStationType() == null || r.getStationType().isBlank()
                        || r.getStationType().equalsIgnoreCase(station.getStationType()))
                .sorted((r1, r2) -> {
                    boolean r1Specific = r1.getStationType() != null && !r1.getStationType().isBlank();
                    boolean r2Specific = r2.getStationType() != null && !r2.getStationType().isBlank();
                    if (r1Specific != r2Specific) {
                        return r1Specific ? -1 : 1;
                    }
                    return r2.getId().compareTo(r1.getId());
                })
                .findFirst();

        if (matchedMember.isEmpty()) {
            matchedMember = activeRules.stream()
                    .filter(r -> "MEMBER".equalsIgnoreCase(r.getRateType()))
                    .findFirst();
        }

        BigDecimal memberMultiplier = matchedMember
                .map(PricingRule::getMultiplier)
                .orElse(BigDecimal.valueOf(1.0));

        return hourlyRate
                .multiply(peakOffPeakMultiplier)
                .multiply(memberMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<PricingRule> getAllRules() {
        return pricingRuleRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Optional<PricingRule> getRuleById(Long id) {
        return pricingRuleRepository.findById(id);
    }

    @Transactional
    public PricingRule saveRule(PricingRule rule) {
        if (rule.getStationType() != null && rule.getStationType().trim().isEmpty()) {
            rule.setStationType(null);
        }
        if (rule.getStartHour() == null || rule.getEndHour() == null || rule.getStartHour() >= rule.getEndHour()) {
            throw new IllegalArgumentException("Start hour must be strictly less than end hour.");
        }
        if (rule.getMultiplier() == null || rule.getMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Multiplier must be greater than zero.");
        }
        if (rule.getRateType() == null || (!"PEAK".equalsIgnoreCase(rule.getRateType())
                && !"OFF_PEAK".equalsIgnoreCase(rule.getRateType())
                && !"MEMBER".equalsIgnoreCase(rule.getRateType()))) {
            throw new IllegalArgumentException("Rate type must be PEAK, OFF_PEAK, or MEMBER.");
        }
        return pricingRuleRepository.save(rule);
    }

    @Transactional
    public void deactivateRule(Long id) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));
        rule.setIsActive(false);
        pricingRuleRepository.save(rule);
    }

    @Transactional
    public void toggleRuleActive(Long id) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pricing rule not found: " + id));
        rule.setIsActive(!Boolean.TRUE.equals(rule.getIsActive()));
        pricingRuleRepository.save(rule);
    }
}
