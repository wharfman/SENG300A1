package gocycle.stations;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kiosk text in English, French and Spanish. Unknown languages fall back to English. */
public class InMemoryLocalizationService implements LocalizationService {
    private static final Locale FRENCH = Locale.FRENCH;
    private static final Locale SPANISH = Locale.of("es");

    private final Map<String, Map<String, String>> byLanguage = new HashMap<>();

    public InMemoryLocalizationService() {
        en("kiosk.welcome", "Welcome to {0} station.");
        fr("kiosk.welcome", "Bienvenue à la station {0}.");
        es("kiosk.welcome", "Bienvenido a la estación {0}.");

        en("kiosk.unlocked", "{0} is unlocked from point {1}. A refundable deposit of {2} is on hold. Enjoy your ride!");
        fr("kiosk.unlocked", "{0} est déverrouillé au point {1}. Un dépôt remboursable de {2} est retenu. Bonne route !");
        es("kiosk.unlocked", "{0} está desbloqueado en el punto {1}. Se retiene un depósito reembolsable de {2}. ¡Buen viaje!");

        en("kiosk.station.full", "This station is full. The nearest station with a free point is {0}, {1} km away. Your trip keeps running until the vehicle is locked.");
        fr("kiosk.station.full", "Cette station est pleine. La station la plus proche avec un point libre est {0}, à {1} km. Votre trajet continue jusqu'au verrouillage du véhicule.");
        es("kiosk.station.full", "Esta estación está llena. La estación más cercana con un punto libre es {0}, a {1} km. Su viaje continúa hasta que el vehículo quede bloqueado.");

        en("kiosk.dock.instruction", "Please push {0} into point {1}.");
        fr("kiosk.dock.instruction", "Veuillez pousser {0} dans le point {1}.");
        es("kiosk.dock.instruction", "Empuje {0} en el punto {1}.");

        en("kiosk.trip.ended", "Vehicle locked. Trip ended after {0} min. Charged {1}. Your {2} deposit has been released.");
        fr("kiosk.trip.ended", "Véhicule verrouillé. Trajet terminé après {0} min. Montant facturé : {1}. Votre dépôt de {2} a été libéré.");
        es("kiosk.trip.ended", "Vehículo bloqueado. Viaje terminado tras {0} min. Cargo: {1}. Se liberó su depósito de {2}.");

        en("kiosk.fault.thanks", "Thank you. {0} is now out of service and our maintenance crew has been notified.");
        fr("kiosk.fault.thanks", "Merci. {0} est maintenant hors service et notre équipe d'entretien a été avisée.");
        es("kiosk.fault.thanks", "Gracias. {0} está fuera de servicio y avisamos a nuestro equipo de mantenimiento.");

        en("kiosk.fault.noted", "Thank you. We have noted the problem ({1}) on {0}.");
        fr("kiosk.fault.noted", "Merci. Nous avons noté le problème ({1}) sur {0}.");
        es("kiosk.fault.noted", "Gracias. Registramos el problema ({1}) en {0}.");

        en("error.trip.active", "You already have an active trip ({0}). Please return that vehicle first.");
        fr("error.trip.active", "Vous avez déjà un trajet en cours ({0}). Veuillez d'abord rendre ce véhicule.");
        es("error.trip.active", "Ya tiene un viaje activo ({0}). Devuelva ese vehículo primero.");

        en("error.vehicle.unavailable", "{0} cannot be rented right now ({1}).");
        fr("error.vehicle.unavailable", "{0} ne peut pas être loué pour le moment ({1}).");
        es("error.vehicle.unavailable", "{0} no se puede alquilar ahora ({1}).");

        en("error.battery.low", "{0} has only {1}% battery. Electric vehicles need at least {2}% to rent.");
        fr("error.battery.low", "{0} n'a que {1} % de batterie. Il faut au moins {2} % pour louer un véhicule électrique.");
        es("error.battery.low", "{0} solo tiene {1}% de batería. Los vehículos eléctricos necesitan al menos {2}% para alquilarse.");

        en("error.payment.zone", "{0} is not accepted in the {1} zone. Accepted here: {2}.");
        fr("error.payment.zone", "{0} n'est pas accepté dans la zone {1}. Modes acceptés : {2}.");
        es("error.payment.zone", "{0} no se acepta en la zona {1}. Aceptados aquí: {2}.");

        en("error.not.locked", "The vehicle is not locked at point {0}. Your trip is still running.");
        fr("error.not.locked", "Le véhicule n'est pas verrouillé au point {0}. Votre trajet continue.");
        es("error.not.locked", "El vehículo no está bloqueado en el punto {0}. Su viaje continúa.");

        en("error.lock.failed", "Lock not detected at point {0}. Your trip is still running. Please push the vehicle in again.");
        fr("error.lock.failed", "Verrouillage non détecté au point {0}. Votre trajet continue. Veuillez pousser le véhicule de nouveau.");
        es("error.lock.failed", "No se detectó el bloqueo en el punto {0}. Su viaje continúa. Empuje el vehículo otra vez.");

        en("error.payment.declined", "The payment was declined. Your trip is still running.");
        fr("error.payment.declined", "Le paiement a été refusé. Votre trajet continue.");
        es("error.payment.declined", "El pago fue rechazado. Su viaje continúa.");

        en("error.network.full", "No station has a free point right now. Your trip is still running; please wait here.");
        fr("error.network.full", "Aucune station n'a de point libre pour le moment. Votre trajet continue; veuillez patienter ici.");
        es("error.network.full", "Ninguna estación tiene un punto libre ahora. Su viaje continúa; espere aquí.");
    }

    @Override
    public String translate(String key, Locale locale) {
        String text = lookup(locale.getLanguage(), key);
        if (text == null) {
            text = lookup(Locale.ENGLISH.getLanguage(), key);
        }
        return text != null ? text : key;
    }

    @Override
    public List<Locale> supportedLocales() {
        return List.of(Locale.ENGLISH, FRENCH, SPANISH);
    }

    private String lookup(String language, String key) {
        return byLanguage.getOrDefault(language, Map.of()).get(key);
    }

    private void en(String key, String text) {
        put(Locale.ENGLISH, key, text);
    }

    private void fr(String key, String text) {
        put(FRENCH, key, text);
    }

    private void es(String key, String text) {
        put(SPANISH, key, text);
    }

    private void put(Locale locale, String key, String text) {
        byLanguage.computeIfAbsent(locale.getLanguage(), l -> new HashMap<>()).put(key, text);
    }
}
