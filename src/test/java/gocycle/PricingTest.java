/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Unit tests for the pricing package and the per-rider-kind pricing chosen in the riders package.
 */
package gocycle;

import gocycle.pricing.CasualPricing;
import gocycle.pricing.LongTripSurcharge;
import gocycle.pricing.MemberPricing;
import gocycle.pricing.PricingStrategy;
import gocycle.pricing.StudentPricing;
import gocycle.riders.CasualRider;
import gocycle.riders.Member;
import gocycle.riders.Student;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the fare formula for each rider kind, the casual-rate fallback for lapsed members and the
 * long-trip surcharge. Expected fares are written out as unlock fee + minutes x rate so a changed rate
 * shows up as a failure here.
 */
class PricingTest {
    @Test
    void tripCostsUnlockFeePlusPerMinuteRate() {
        assertEquals(1.25 + 10 * 0.35, new CasualPricing().calculate(10), 1e-9);
        assertEquals(1.25 + 10 * 0.20, new MemberPricing().calculate(10), 1e-9);
        assertEquals(1.25 + 10 * 0.25, new StudentPricing().calculate(10), 1e-9);
    }

    @Test
    void membersAndStudentsPayLessThanCasualRiders() {
        double casual = new CasualRider("R-1", "C", "c@x").getPricingStrategy().calculate(30);
        double member = new Member("R-2", "M", "m@x", 15, LocalDate.now().plusDays(1)).getPricingStrategy().calculate(30);
        double student = new Student("R-3", "S", "s@x", "123", LocalDate.now().plusDays(1)).getPricingStrategy().calculate(30);
        assertTrue(member < casual);
        assertTrue(student < casual);
    }

    // Membership status is checked against the real date (LocalDate.now()), not a simulation clock,
    // so the renewal date is set relative to today.
    @Test
    void lapsedMemberPaysCasualRate() {
        Member lapsed = new Member("R-2", "M", "m@x", 15, LocalDate.now().minusDays(1));
        assertEquals(new CasualPricing().calculate(30), lapsed.getPricingStrategy().calculate(30), 1e-9);
    }

    // 120 minutes is exactly the threshold and is not surcharged; 121 is the first minute that is.
    @Test
    void surchargeAppliesOnlyAfterTwoHours() {
        PricingStrategy pricing = new LongTripSurcharge(new CasualPricing());
        assertEquals(new CasualPricing().calculate(120), pricing.calculate(120), 1e-9);
        assertEquals(new CasualPricing().calculate(121) + LongTripSurcharge.DEFAULT_SURCHARGE, pricing.calculate(121), 1e-9);
    }
}
