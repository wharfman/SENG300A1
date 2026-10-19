/*
 * GoCycle - SENG 300 Assignment 1
 *
 * One of the three rider kinds; registered by GoCycleSystem and priced through StudentPricing while
 * verified.
 */
package gocycle.riders;

import gocycle.common.Message;
import gocycle.pricing.CasualPricing;
import gocycle.pricing.LongTripSurcharge;
import gocycle.pricing.PricingStrategy;
import gocycle.pricing.StudentPricing;

import java.time.LocalDate;
import java.util.Objects;

/** Verified student; gets the reduced rate until the verification expires. */
public class Student extends Rider {
    private final String studentNumber;
    private final LocalDate verifiedUntil;

    /** @param verifiedUntil last day the student rate applies */
    public Student(String riderId, String name, String email, String studentNumber, LocalDate verifiedUntil) {
        super(riderId, name, email);
        this.studentNumber = Objects.requireNonNull(studentNumber);
        this.verifiedUntil = Objects.requireNonNull(verifiedUntil);
    }

    /** Verified up to and including {@code verifiedUntil}, judged by the real date rather than the simulation clock. */
    public boolean isVerified() {
        return !LocalDate.now().isAfter(verifiedUntil);
    }

    /** An expired verification falls back to the casual rate. */
    @Override
    public PricingStrategy getPricingStrategy() {
        return new LongTripSurcharge(isVerified() ? new StudentPricing() : new CasualPricing());
    }

    @Override
    public Message getKind() {
        return new Message(isVerified() ? "rider.kind.student" : "rider.kind.student.unverified");
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public LocalDate getVerifiedUntil() {
        return verifiedUntil;
    }
}
