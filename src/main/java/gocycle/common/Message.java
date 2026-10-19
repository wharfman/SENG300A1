/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Shared by every package that produces rider-facing text (pricing, payments, riders) without depending on
 * the stations package, where LocalizationService turns it into the current language.
 */
package gocycle.common;

/**
 * Text that has not been translated yet: a message key plus the values for its {@code {0}}, {@code {1}}, ...
 * placeholders. An argument may itself be a {@code Message} or an enum constant; both are translated too.
 *
 * <p>Because {@code args} is an array, the record's generated {@code equals} and {@code hashCode} compare it
 * by identity, not by content; messages are only rendered, never compared.
 */
public record Message(String key, Object... args) {
}
