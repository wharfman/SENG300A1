/*
 * GoCycle - SENG 300 Assignment 1
 *
 * Tests for the English/French message catalog and the global language setting, run against TestNetwork.
 */
package gocycle;

import gocycle.common.Message;
import gocycle.faults.FaultType;
import gocycle.fleet.Vehicle;
import gocycle.fleet.VehicleStatus;
import gocycle.fleet.VehicleType;
import gocycle.payments.CreditCardPayment;
import gocycle.payments.PaymentType;
import gocycle.riders.ReceiptDelivery;
import gocycle.stations.DockingPoint;
import gocycle.stations.InMemoryLocalizationService;
import gocycle.stations.StationKiosk;
import gocycle.trips.Receipt;
import gocycle.trips.Trip;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the catalog's completeness (both languages define the same keys, every enum constant and every key
 * literal in the source has a translation) and that switching the one global language changes kiosk text,
 * fares and receipts.
 */
class LocalizationTest {
    private static final List<Class<? extends Enum<?>>> TRANSLATED_ENUMS = List.of(
            VehicleType.class, VehicleStatus.class, PaymentType.class, FaultType.class, ReceiptDelivery.class);
    // A quoted, lowercase dotted literal such as "kiosk.welcome" is treated as a message key.
    private static final Pattern KEY_LITERAL = Pattern.compile("\"([a-z]+(?:\\.[a-z0-9]+)+)\"");

    private final InMemoryLocalizationService l10n = new InMemoryLocalizationService();

    @Test
    void englishAndFrenchDefineTheSameKeys() {
        assertEquals(new TreeSet<>(l10n.keys(Locale.ENGLISH)), new TreeSet<>(l10n.keys(Locale.FRENCH)));
    }

    @Test
    void everyEnumConstantShownToRidersIsTranslated() {
        for (Class<? extends Enum<?>> type : TRANSLATED_ENUMS) {
            for (Enum<?> constant : type.getEnumConstants()) {
                String key = type.getSimpleName() + "." + constant.name();
                assertTrue(l10n.keys(Locale.ENGLISH).contains(key), "missing " + key);
            }
        }
    }

    @Test
    void everyKeyLiteralInTheSourceIsDefined() throws IOException {
        Set<String> keys = l10n.keys(Locale.ENGLISH);
        // Only literals in a key family ("kiosk.", "menu.", ...) count, so file names like "zones.properties" don't.
        Set<String> families = keys.stream().map(k -> k.substring(0, k.indexOf('.') + 1)).collect(Collectors.toSet());
        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = KEY_LITERAL.matcher(Files.readString(file));
                while (m.find()) {
                    String literal = m.group(1);
                    boolean inFamily = families.contains(literal.substring(0, literal.indexOf('.') + 1));
                    if (inFamily && !keys.contains(literal)) {
                        missing.add(m.group(1) + " in " + file.getFileName());
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "undefined keys: " + missing);
    }

    @Test
    void onlyEnglishAndFrenchCanBeSelected() {
        assertEquals(List.of(Locale.ENGLISH, Locale.FRENCH), l10n.supportedLocales());
        assertThrows(IllegalArgumentException.class, () -> l10n.setLanguage(Locale.of("es")));
        assertEquals(Locale.ENGLISH, l10n.getLanguage());
    }

    @Test
    void nestedMessagesEnumsAndListsAreTranslated() {
        Message fare = new Message("fare.surcharge", new Message("fare.base", "$1.25", "$0.35"), "$5.00", 120);
        l10n.setLanguage(Locale.FRENCH);

        assertEquals("$1.25 de déverrouillage + $0.35/min, +$5.00 au-delà de 120 min", l10n.render(fare));
        assertEquals("Carte de crédit, Portefeuille mobile",
                l10n.render(List.of(PaymentType.CREDIT_CARD, PaymentType.PHONE_WALLET)));
        assertEquals("Vélo électrique hors service",
                l10n.text("{0} {1}", VehicleType.E_BIKE, "hors service"));
    }

    @Test
    void kioskFollowsTheGlobalLanguage() {
        ByteArrayOutputStream screen = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(screen, true, StandardCharsets.UTF_8);
        StationKiosk kiosk = new StationKiosk(l10n, out);

        kiosk.announce("error.vehicle.unavailable", "EB-1", VehicleStatus.OUT_OF_SERVICE);
        l10n.setLanguage(Locale.FRENCH);
        kiosk.announce("error.vehicle.unavailable", "EB-1", VehicleStatus.OUT_OF_SERVICE);

        String text = screen.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("EB-1 cannot be rented right now (Out of service)."), text);
        assertTrue(text.contains("EB-1 ne peut pas être loué pour le moment (Hors service)."), text);
    }

    @Test
    void receiptIsShownInTheCurrentLanguage() {
        TestNetwork net = new TestNetwork();
        Vehicle bike = net.park(net.a, VehicleType.PEDAL_BIKE, "PB-1", 0);
        Trip trip = net.trips.startTrip(net.rider, net.a, bike, new CreditCardPayment("4111 1111 1111 1111"));
        net.advanceMinutes(10);
        net.trips.requestReturn(trip, net.b);
        DockingPoint point = net.b.getFreePoints().get(0);
        point.dock(bike);
        Receipt receipt = net.trips.confirmDocked(trip, point);

        String english = receipt.toText(l10n);
        l10n.setLanguage(Locale.FRENCH);
        String french = receipt.toText(l10n);

        assertTrue(english.contains("Vehicle   PB-1 (Pedal bike)"), english);
        assertTrue(english.contains("Charged   $4.75 to Credit card **** 1111"), english);
        assertTrue(french.contains("Véhicule  PB-1 (Vélo classique)"), french);
        assertTrue(french.contains("Tarif     $1.25 de déverrouillage + $0.35/min, +$5.00 au-delà de 120 min"), french);
        assertTrue(french.contains("Facturé   $4.75 sur Carte de crédit **** 1111"), french);
    }
}
