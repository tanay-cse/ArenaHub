package com.arenahub.booking;

import com.arenahub.catalog.Station;
import com.arenahub.catalog.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private PricingService pricingService;

    private Station stationPc;

    @BeforeEach
    void setUp() {
        stationPc = new Station();
        stationPc.setId(1L);
        stationPc.setStationNumber("PC-01");
        stationPc.setStationType("PC");
        stationPc.setHourlyRate(new BigDecimal("100.00"));
        stationPc.setStatus("AVAILABLE");

        when(stationRepository.findById(1L)).thenReturn(Optional.of(stationPc));
    }

    private PricingRule createRule(Long id, String name, String rateType, String stationType, int startHour, int endHour, BigDecimal multiplier) {
        PricingRule rule = new PricingRule();
        rule.setId(id);
        rule.setRuleName(name);
        rule.setRateType(rateType);
        rule.setStationType(stationType);
        rule.setStartHour((byte) startHour);
        rule.setEndHour((byte) endHour);
        rule.setMultiplier(multiplier);
        rule.setIsActive(true);
        return rule;
    }

    @Test
    @DisplayName("Default: When no pricing rules apply, uses 1.0 multipliers and returns the base hourly rate")
    void testNoRuleAppliesDefault() {
        when(pricingRuleRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        BigDecimal calculatedPrice = pricingService.calculatePrice(1L, 14);

        assertEquals(new BigDecimal("100.00"), calculatedPrice,
                "When no active rules exist, default multiplier 1.0 applies to base rate 100.00");
    }

    @Test
    @DisplayName("Peak multiplier alone: Active PEAK rule (1.5x) during peak hours applies to base rate")
    void testPeakMultiplierAlone() {
        PricingRule peakRule = createRule(1L, "Evening Peak", "PEAK", null, 17, 22, new BigDecimal("1.50"));
        when(pricingRuleRepository.findByIsActiveTrue()).thenReturn(List.of(peakRule));

        // Inside peak window (18:00): 100.00 * 1.50 * 1.0 (no member rule) = 150.00
        BigDecimal peakPrice = pricingService.calculatePrice(1L, 18);
        assertEquals(new BigDecimal("150.00"), peakPrice);

        // Outside peak window (15:00): 100.00 * 1.0 = 100.00
        BigDecimal normalPrice = pricingService.calculatePrice(1L, 15);
        assertEquals(new BigDecimal("100.00"), normalPrice);
    }

    @Test
    @DisplayName("Off-peak multiplier alone: Active OFF_PEAK rule (0.8x) during morning hours applies to base rate")
    void testOffPeakMultiplierAlone() {
        PricingRule offPeakRule = createRule(2L, "Morning Discount", "OFF_PEAK", null, 10, 14, new BigDecimal("0.80"));
        when(pricingRuleRepository.findByIsActiveTrue()).thenReturn(List.of(offPeakRule));

        // Inside off-peak window (11:00): 100.00 * 0.80 * 1.0 = 80.00
        BigDecimal offPeakPrice = pricingService.calculatePrice(1L, 11);
        assertEquals(new BigDecimal("80.00"), offPeakPrice);

        // Outside off-peak window (16:00): 100.00 * 1.0 = 100.00
        BigDecimal normalPrice = pricingService.calculatePrice(1L, 16);
        assertEquals(new BigDecimal("100.00"), normalPrice);
    }

    @Test
    @DisplayName("Multipliers combine: PEAK (1.5x) and MEMBER (0.9x) combine multiplicatively (100 * 1.5 * 0.9 = 135.00)")
    void testPeakAndMemberMultipliersCombine() {
        PricingRule peakRule = createRule(1L, "Evening Peak", "PEAK", null, 17, 22, new BigDecimal("1.50"));
        PricingRule memberRule = createRule(3L, "Member Loyalty", "MEMBER", null, 0, 24, new BigDecimal("0.90"));
        when(pricingRuleRepository.findByIsActiveTrue()).thenReturn(List.of(peakRule, memberRule));

        // At peak hour 19: 100.00 * 1.50 * 0.90 = 135.00
        BigDecimal combinedPrice = pricingService.calculatePrice(1L, 19);
        assertEquals(new BigDecimal("135.00"), combinedPrice);
    }

    @Test
    @DisplayName("Multipliers combine: OFF_PEAK (0.8x) and MEMBER (0.9x) combine multiplicatively (100 * 0.8 * 0.9 = 72.00)")
    void testOffPeakAndMemberMultipliersCombine() {
        PricingRule offPeakRule = createRule(2L, "Morning Discount", "OFF_PEAK", null, 10, 14, new BigDecimal("0.80"));
        PricingRule memberRule = createRule(3L, "Member Loyalty", "MEMBER", null, 0, 24, new BigDecimal("0.90"));
        when(pricingRuleRepository.findByIsActiveTrue()).thenReturn(List.of(offPeakRule, memberRule));

        // At off-peak hour 12: 100.00 * 0.80 * 0.90 = 72.00
        BigDecimal combinedPrice = pricingService.calculatePrice(1L, 12);
        assertEquals(new BigDecimal("72.00"), combinedPrice);
    }

    @Test
    @DisplayName("Precedence: Station-specific rule takes precedence over generic station rule")
    void testStationSpecificRulePrecedence() {
        PricingRule genericPeak = createRule(1L, "Generic Peak", "PEAK", null, 17, 22, new BigDecimal("1.20"));
        PricingRule specificPcPeak = createRule(2L, "PC VIP Peak", "PEAK", "PC", 17, 22, new BigDecimal("1.40"));
        PricingRule memberRule = createRule(3L, "Member Loyalty", "MEMBER", null, 0, 24, new BigDecimal("0.90"));

        when(pricingRuleRepository.findByIsActiveTrue()).thenReturn(List.of(genericPeak, specificPcPeak, memberRule));

        // For PC station at hour 18: Specific PC peak (1.40) is picked over generic (1.20)
        // 100.00 * 1.40 * 0.90 = 126.00
        BigDecimal pcPrice = pricingService.calculatePrice(1L, 18);
        assertEquals(new BigDecimal("126.00"), pcPrice);
    }
}
