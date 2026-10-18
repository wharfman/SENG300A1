package gocycle.fleet;

/** Why a vehicle can't be rented, as a kiosk message key plus its arguments. */
public record RentalRefusal(String messageKey, Object... args) {
}
