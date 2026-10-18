package gocycle.riders;

import gocycle.pricing.CasualPricing;
import gocycle.pricing.LongTripSurcharge;
import gocycle.pricing.MemberPricing;
import gocycle.pricing.PricingStrategy;

import java.time.LocalDate;
import java.util.Objects;

/** Pays monthly and gets the lower per-minute rate while the membership is active. */
public class Member extends Rider {
    private final double monthlyFee;
    private final LocalDate renewalDate;

    public Member(String riderId, String name, String email, double monthlyFee, LocalDate renewalDate) {
        super(riderId, name, email);
        this.monthlyFee = monthlyFee;
        this.renewalDate = Objects.requireNonNull(renewalDate);
    }

    public boolean isActive() {
        return !LocalDate.now().isAfter(renewalDate);
    }

    /** A lapsed membership falls back to the casual rate. */
    @Override
    public PricingStrategy getPricingStrategy() {
        return new LongTripSurcharge(isActive() ? new MemberPricing() : new CasualPricing());
    }

    @Override
    public String getKind() {
        return isActive() ? "Member" : "Member (lapsed)";
    }

    public double getMonthlyFee() {
        return monthlyFee;
    }

    public LocalDate getRenewalDate() {
        return renewalDate;
    }
}
