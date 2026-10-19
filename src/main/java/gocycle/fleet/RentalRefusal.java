/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Returned by Vehicle.checkRentable(); TripService turns it into a TripException so the station kiosk
 * can show the reason in the system language.
 */
package gocycle.fleet;

/**
 * Why a vehicle can't be rented, as a kiosk message key plus its arguments. The arguments fill the
 * {@code {0}}, {@code {1}}, ... placeholders of the localized message, in order.
 *
 * <p>Because {@code args} is an array, the record's generated {@code equals} and {@code hashCode}
 * compare it by identity, not by content; refusals are only read, never compared.
 */
public record RentalRefusal(String messageKey, Object... args) {
}
