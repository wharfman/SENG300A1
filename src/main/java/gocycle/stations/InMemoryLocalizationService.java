/*
 * GoCycle - SENG 300 Assignment 1
 *
 * The message table behind every screen; GoCycleSystem creates one instance and shares it across the rider
 * app, the console and all stations. Any new message key must be added here in both languages.
 */
package gocycle.stations;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Screen text in English and French, built once in the constructor and read-only afterwards. Lookups match
 * on language only (a {@code fr_CA} locale gets the French text). Keys missing in the current language fall
 * back to English; a key missing everywhere is returned as-is so the gap shows on screen instead of failing.
 *
 * <p>The language starts as English. It is volatile because the clock thread renders rider notices while
 * the menu thread may be changing it.
 */
public class InMemoryLocalizationService implements LocalizationService {
    private static final Locale FRENCH = Locale.FRENCH;

    private final Map<String, Map<String, String>> byLanguage = new HashMap<>();
    private volatile Locale language = Locale.ENGLISH;

    public InMemoryLocalizationService() {
        kioskMessages();
        appMessages();
        domainLabels();
    }

    private void kioskMessages() {
        en("kiosk.welcome", "Welcome to {0} station.");
        fr("kiosk.welcome", "Bienvenue à la station {0}.");

        en("kiosk.unlocked", "{0} is unlocked from point {1}. A refundable deposit of {2} is on hold. Enjoy your ride!");
        fr("kiosk.unlocked", "Le véhicule {0} est déverrouillé au point {1}. Un dépôt remboursable de {2} est retenu. Bonne route !");

        en("kiosk.station.full", "This station is full. The nearest station with a free point is {0}, {1} km away. Your trip keeps running until the vehicle is locked.");
        fr("kiosk.station.full", "Cette station est pleine. La station la plus proche avec un point libre est {0}, à {1} km. Votre trajet continue jusqu'au verrouillage du véhicule.");

        en("kiosk.dock.instruction", "Please push {0} into point {1}.");
        fr("kiosk.dock.instruction", "Veuillez pousser {0} dans le point {1}.");

        en("kiosk.trip.ended", "Vehicle locked. Trip ended after {0} min. Charged {1}. Your {2} deposit has been released.");
        fr("kiosk.trip.ended", "Véhicule verrouillé. Trajet terminé après {0} min. Montant facturé : {1}. Votre dépôt de {2} a été libéré.");

        en("kiosk.fault.thanks", "Thank you. {0} is now out of service and our maintenance crew has been notified.");
        fr("kiosk.fault.thanks", "Merci. Le véhicule {0} est maintenant hors service et notre équipe d'entretien a été avisée.");

        en("kiosk.fault.noted", "Thank you. We have noted the problem ({1}) on {0}.");
        fr("kiosk.fault.noted", "Merci. Nous avons noté le problème ({1}) sur le véhicule {0}.");

                en("error.trip.active", "You already have an active trip ({0}). Please return that vehicle first.");
        fr("error.trip.active", "Vous avez déjà un trajet en cours ({0}). Veuillez d'abord rendre ce véhicule.");

        en("error.vehicle.unavailable", "{0} cannot be rented right now ({1}).");
        fr("error.vehicle.unavailable", "Le véhicule {0} ne peut pas être loué pour le moment ({1}).");

        en("error.vehicle.not.docked", "{0} is not docked at this station.");
        fr("error.vehicle.not.docked", "Le véhicule {0} n'est pas amarré à cette station.");

        en("error.battery.low", "{0} has only {1}% battery. Electric vehicles need at least {2}% to rent.");
        fr("error.battery.low", "Le véhicule {0} n'a que {1} % de batterie. Il faut au moins {2} % pour louer un véhicule électrique.");

        en("error.payment.zone", "{0} is not accepted in the {1} zone. Accepted here: {2}.");
        fr("error.payment.zone", "Mode de paiement refusé dans la zone {1} : {0}. Modes acceptés : {2}.");

        en("error.not.locked", "The vehicle is not locked at point {0}. Your trip is still running.");
        fr("error.not.locked", "Le véhicule n'est pas verrouillé au point {0}. Votre trajet continue.");

        en("error.lock.failed", "Lock not detected at point {0}. Your trip is still running. Please push the vehicle in again.");
        fr("error.lock.failed", "Verrouillage non détecté au point {0}. Votre trajet continue. Veuillez pousser le véhicule de nouveau.");

        en("error.payment.declined", "The payment was declined, so the vehicle has been unlocked again. Your trip is still running.");
        fr("error.payment.declined", "Le paiement a été refusé : le véhicule a donc été déverrouillé. Votre trajet continue.");

        en("error.network.full", "No station has a free point right now. Your trip is still running; please wait here.");
        fr("error.network.full", "Aucune station n'a de point libre pour le moment. Votre trajet continue ; veuillez patienter ici.");
    }

    /** Text printed by RiderApp and Console. Prompts keep their trailing ": " so input lines up after them. */
    private void appMessages() {
        en("app.welcome", "Welcome to GoCycle.");
        fr("app.welcome", "Bienvenue chez GoCycle.");
        en("app.goodbye", "Goodbye.");
        fr("app.goodbye", "Au revoir.");
        en("app.choose", "Choose 1-9: ");
        fr("app.choose", "Choisissez 1-9 : ");

        en("menu.1", "Sign up or switch rider");
        fr("menu.1", "S'inscrire ou changer d'usager");
        en("menu.2", "Find stations and vehicles");
        fr("menu.2", "Trouver des stations et des véhicules");
        en("menu.3", "Start a trip");
        fr("menu.3", "Commencer un trajet");
        en("menu.4", "Ride - let time pass");
        fr("menu.4", "Rouler - laisser passer le temps");
        en("menu.5", "Return my vehicle");
        fr("menu.5", "Rendre mon véhicule");
        en("menu.6", "Report a problem with a vehicle");
        fr("menu.6", "Signaler un problème avec un véhicule");
        en("menu.7", "My trips and receipts");
        fr("menu.7", "Mes trajets et reçus");
        en("menu.8", "Language and receipt settings");
        fr("menu.8", "Langue et reçus");
        en("menu.9", "Exit");
        fr("menu.9", "Quitter");

        en("console.number.range", "Please enter a number from {0} to {1}.");
        fr("console.number.range", "Veuillez entrer un nombre de {0} à {1}.");
        en("console.yes.no", " (y/n): ");
        fr("console.yes.no", " (o/n) : ");
        en("console.back", "Back");
        fr("console.back", "Retour");
        en("console.pause", "Press Enter to return to the menu...");
        fr("console.pause", "Appuyez sur Entrée pour revenir au menu...");

        en("rider.label", "{0} ({1})");
        fr("rider.label", "{0} ({1})");
        en("rider.signed.in", "(signed in)");
        fr("rider.signed.in", "(connecté)");
        en("rider.who", "Who is riding?");
        fr("rider.who", "Qui utilise GoCycle ?");
        en("rider.signup.casual", "Sign up as a casual rider - pay per trip");
        fr("rider.signup.casual", "S'inscrire comme usager occasionnel - payer par trajet");
        en("rider.signup.member", "Sign up as a member - {0}/month, lower per-minute rate");
        fr("rider.signup.member", "S'inscrire comme membre - {0}/mois, tarif à la minute réduit");
        en("rider.signup.student", "Sign up as a student - reduced rate");
        fr("rider.signup.student", "S'inscrire comme étudiant - tarif réduit");
        en("rider.prompt.name", "Full name: ");
        fr("rider.prompt.name", "Nom complet : ");
        en("rider.prompt.email", "Email: ");
        fr("rider.prompt.email", "Courriel : ");
        en("rider.prompt.student.number", "Student number: ");
        fr("rider.prompt.student.number", "Numéro d'étudiant : ");
        en("rider.welcome", "Welcome, {0}. Your fare: {1}");
        fr("rider.welcome", "Bienvenue, {0}. Votre tarif : {1}");

        en("stations.zone", " {0} zone ");
        fr("stations.zone", " Zone {0} ");
        en("stations.accepts", "  accepts {0}");
        fr("stations.accepts", "  accepte {0}");
        en("stations.counts", "{0} ready, {1} free point(s)");
        fr("stations.counts", "{0} disponible(s), {1} point(s) libre(s)");
        en("stations.point.free", "free");
        fr("stations.point.free", "libre");
        en("stations.ask.start", "Which station are you at?");
        fr("stations.ask.start", "À quelle station êtes-vous ?");
        en("stations.ask.return", "Which station are you returning to?");
        fr("stations.ask.return", "À quelle station rendez-vous le véhicule ?");
        en("stations.ask.fault", "Which station is it at?");
        fr("stations.ask.fault", "À quelle station se trouve-t-il ?");

        en("vehicle.battery", "battery {0}%");
        fr("vehicle.battery", "batterie {0} %");
        en("vehicle.ready", "ready");
        fr("vehicle.ready", "disponible");
        en("vehicle.battery.too.low", "battery too low to rent");
        fr("vehicle.battery.too.low", "batterie trop faible pour louer");

        en("start.already.riding", "You are already on trip {0}. Return that vehicle first (option 5).");
        fr("start.already.riding", "Vous êtes déjà en trajet ({0}). Rendez d'abord ce véhicule (option 5).");
        en("start.no.vehicles", "No vehicles are docked here right now.");
        fr("start.no.vehicles", "Aucun véhicule n'est amarré ici pour le moment.");
        en("start.choose.vehicle", "Choose a vehicle");
        fr("start.choose.vehicle", "Choisissez un véhicule");

        en("pay.ask", "How would you like to pay?");
        fr("pay.ask", "Comment souhaitez-vous payer ?");
        en("pay.not.accepted", "(not accepted in {0})");
        fr("pay.not.accepted", "(mode refusé dans {0})");
        en("pay.credit.number", "Credit card number");
        fr("pay.credit.number", "Numéro de carte de crédit");
        en("pay.transit.number", "Transit card number");
        fr("pay.transit.number", "Numéro de carte de transport");
        en("pay.card.prompt", "{0} (Enter for demo card {1}): ");
        fr("pay.card.prompt", "{0} (Entrée pour la carte démo {1}) : ");
        en("pay.card.too.short", "Please enter at least 4 digits.");
        fr("pay.card.too.short", "Veuillez entrer au moins 4 chiffres.");
        en("pay.wallet.paired", "Phone wallet paired.");
        fr("pay.wallet.paired", "Portefeuille mobile jumelé.");

        en("time.background", "Time also passes on its own: every real second is {0} network seconds.");
        fr("time.background", "Le temps passe aussi tout seul : chaque seconde réelle vaut {0} secondes sur le réseau.");
        en("time.ask", "How many minutes should pass? (1-240): ");
        fr("time.ask", "Combien de minutes doivent passer ? (1-240) : ");
        en("time.passed", "{0} minute(s) passed.");
        fr("time.passed", "{0} minute(s) écoulée(s).");

        en("return.no.trip", "You have no active trip.");
        fr("return.no.trip", "Vous n'avez aucun trajet en cours.");
        en("return.ride.on", "Ride on to {0} now?");
        fr("return.ride.on", "Continuer vers {0} maintenant ?");
        en("return.still.running", "Your trip is still running and charging until the vehicle is locked.");
        fr("return.still.running", "Votre trajet continue et reste facturé jusqu'au verrouillage du véhicule.");
        en("return.riding.to", "Riding to {0} ({1} min)...");
        fr("return.riding.to", "En route vers {0} ({1} min)...");
        en("return.point.taken", "Someone just took the last free point. Choose option 5 again to be sent to the nearest free point.");
        fr("return.point.taken", "Quelqu'un vient de prendre le dernier point libre. Choisissez de nouveau l'option 5 pour être dirigé vers le point libre le plus proche.");
        en("return.push", "Press Enter to push the vehicle into point {0}...");
        fr("return.push", "Appuyez sur Entrée pour pousser le véhicule dans le point {0}...");
        en("return.receipt", "Receipt {0}.");
        fr("return.receipt", "Reçu {0}.");
        en("return.receipt.app", "sent to your app inbox (option 7)");
        fr("return.receipt.app", "envoyé dans la boîte de réception de l'application (option 7)");
        en("return.receipt.email", "emailed to {0} (saved in {1})");
        fr("return.receipt.email", "envoyé par courriel à {0} (enregistré dans {1})");
        en("return.receipt.and", " and ");
        fr("return.receipt.and", " et ");
        en("return.receipt.email.failed", "Your receipt could not be emailed to {0}; it is shown above.");
        fr("return.receipt.email.failed", "Votre reçu n'a pas pu être envoyé par courriel à {0} ; il est affiché ci-dessus.");

        en("fault.riding", "The vehicle I'm riding ({0})");
        fr("fault.riding", "Le véhicule que j'utilise ({0})");
        en("fault.docked", "A vehicle docked at a station");
        fr("fault.docked", "Un véhicule amarré à une station");
        en("fault.ask.where", "Which vehicle has the problem?");
        fr("fault.ask.where", "Quel véhicule a un problème ?");
        en("fault.none.docked", "No vehicles are docked there.");
        fr("fault.none.docked", "Aucun véhicule n'est amarré à cette station.");
        en("fault.ask.vehicle", "Which vehicle?");
        fr("fault.ask.vehicle", "Quel véhicule ?");
        en("fault.ask.type", "What is wrong?");
        fr("fault.ask.type", "Quel est le problème ?");
        en("fault.ask.note", "Anything else we should know? (optional): ");
        fr("fault.ask.note", "Autre chose à signaler ? (facultatif) : ");
        en("fault.filed", "Report {0} filed. The control room can see it now.");
        fr("fault.filed", "Rapport {0} enregistré. La salle de contrôle peut le voir maintenant.");
        en("fault.return.vehicle", "Please return {0} to a station; it goes out of service once docked.");
        fr("fault.return.vehicle", "Veuillez rapporter le véhicule {0} à une station ; il sera mis hors service une fois amarré.");
        en("fault.notice", "Your {0} {1} reported: {2} ({3}% battery)");
        fr("fault.notice", "Le véhicule {1} ({0}) a signalé : {2} (batterie {3} %)");

        en("trips.title", "Trips for {0}");
        fr("trips.title", "Trajets : {0}");
        en("trips.none", "No trips yet.");
        fr("trips.none", "Aucun trajet pour l'instant.");
        en("trips.in.progress", "in progress, {0} so far");
        fr("trips.in.progress", "en cours, {0} jusqu'ici");
        en("trips.paid", "{0} paid");
        fr("trips.paid", "{0} payé");
        en("trips.inbox", "App inbox: {0} receipt(s)");
        fr("trips.inbox", "Boîte de réception : {0} reçu(s)");
        en("trips.email.folder", "Emailed receipts are saved in {0}");
        fr("trips.email.folder", "Les reçus envoyés par courriel sont enregistrés dans {0}");

        en("settings.title", "Settings for {0}");
        fr("settings.title", "Paramètres : {0}");
        en("settings.language", "Language: {0}");
        fr("settings.language", "Langue : {0}");
        en("settings.receipts", "Receipts: {0}");
        fr("settings.receipts", "Reçus : {0}");
        en("settings.choose.language", "Screen language");
        fr("settings.choose.language", "Langue d'affichage");
        en("settings.choose.receipts", "Send receipts to");
        fr("settings.choose.receipts", "Envoyer les reçus à");

        en("status.no.trip", "No active trip.");
        fr("status.no.trip", "Aucun trajet en cours.");
        en("status.trip", "Trip {0} ({1}): ");
        fr("status.trip", "Trajet {0} ({1}) : ");
        en("status.riding", "riding");
        fr("status.riding", "en route");
        en("status.docking", "docking at {0}");
        fr("status.docking", "amarrage à {0}");
        en("status.from", "from {0}");
        fr("status.from", "depuis {0}");
        en("status.so.far", "{0} so far");
        fr("status.so.far", "{0} jusqu'ici");
        en("status.deposit", "{0} deposit held");
        fr("status.deposit", "dépôt de {0} retenu");

        // Receipt labels are padded so the values line up in a column within each language.
        en("receipt.title", "GoCycle receipt {0}");
        fr("receipt.title", "Reçu GoCycle {0}");
        en("receipt.vehicle", "  Vehicle   {0} ({1})");
        fr("receipt.vehicle", "  Véhicule  {0} ({1})");
        en("receipt.route", "  Route     {0} -> {1}");
        fr("receipt.route", "  Trajet    {0} -> {1}");
        en("receipt.duration", "  Duration  {0} min");
        fr("receipt.duration", "  Durée     {0} min");
        en("receipt.fare", "  Fare      {0}");
        fr("receipt.fare", "  Tarif     {0}");
        en("receipt.charged", "  Charged   {0} to {1}");
        fr("receipt.charged", "  Facturé   {0} sur {1}");
        en("receipt.deposit", "  Deposit   {0} hold released");
        fr("receipt.deposit", "  Dépôt     retenue de {0} libérée");
        en("receipt.issued", "  Issued    {0}");
        fr("receipt.issued", "  Émis le   {0}");
        en("email.to", "To: {0}");
        fr("email.to", "À : {0}");
        en("email.subject", "Subject: Your GoCycle receipt for {0}");
        fr("email.subject", "Objet : Votre reçu GoCycle pour {0}");
    }

    /** Messages built by the domain packages, and enum constants keyed {@code <SimpleClassName>.<CONSTANT>}. */
    private void domainLabels() {
        en("fare.base", "{0} unlock + {1}/min");
        fr("fare.base", "{0} de déverrouillage + {1}/min");
        en("fare.surcharge", "{0}, +{1} over {2} min");
        fr("fare.surcharge", "{0}, +{1} au-delà de {2} min");

        en("payment.card", "{0} {1}");
        fr("payment.card", "{0} {1}");
        en("payment.wallet", "{0}");
        fr("payment.wallet", "{0}");

        en("rider.kind.casual", "Casual");
        fr("rider.kind.casual", "Occasionnel");
        en("rider.kind.member", "Member");
        fr("rider.kind.member", "Membre");
        en("rider.kind.member.lapsed", "Member (lapsed)");
        fr("rider.kind.member.lapsed", "Membre (expiré)");
        en("rider.kind.student", "Student");
        fr("rider.kind.student", "Étudiant");
        en("rider.kind.student.unverified", "Student (unverified)");
        fr("rider.kind.student.unverified", "Étudiant (non vérifié)");

        en("VehicleType.PEDAL_BIKE", "Pedal bike");
        fr("VehicleType.PEDAL_BIKE", "Vélo");
        en("VehicleType.E_BIKE", "E-bike");
        fr("VehicleType.E_BIKE", "Vélo électrique");
        en("VehicleType.E_SCOOTER", "E-scooter");
        fr("VehicleType.E_SCOOTER", "Trottinette électrique");

        en("VehicleStatus.AVAILABLE", "Available");
        fr("VehicleStatus.AVAILABLE", "Disponible");
        en("VehicleStatus.IN_USE", "In use");
        fr("VehicleStatus.IN_USE", "En utilisation");
        en("VehicleStatus.OUT_OF_SERVICE", "Out of service");
        fr("VehicleStatus.OUT_OF_SERVICE", "Hors service");
        en("VehicleStatus.IN_TRANSIT", "In transit");
        fr("VehicleStatus.IN_TRANSIT", "En transit");

        en("PaymentType.CREDIT_CARD", "Credit card");
        fr("PaymentType.CREDIT_CARD", "Carte de crédit");
        en("PaymentType.TRANSIT_CARD", "Transit card");
        fr("PaymentType.TRANSIT_CARD", "Carte de transport");
        en("PaymentType.PHONE_WALLET", "Phone wallet");
        fr("PaymentType.PHONE_WALLET", "Portefeuille mobile");

        en("FaultType.FLAT_TIRE", "Flat tire");
        fr("FaultType.FLAT_TIRE", "Pneu crevé");
        en("FaultType.BROKEN_BRAKES", "Broken brakes");
        fr("FaultType.BROKEN_BRAKES", "Freins défectueux");
        en("FaultType.LOW_BATTERY", "Low battery");
        fr("FaultType.LOW_BATTERY", "Batterie faible");
        en("FaultType.SYSTEM_FAULT", "System fault");
        fr("FaultType.SYSTEM_FAULT", "Défaillance du système");
        en("FaultType.OTHER", "Other problem");
        fr("FaultType.OTHER", "Autre problème");

        en("ReceiptDelivery.APP", "Phone app");
        fr("ReceiptDelivery.APP", "Application mobile");
        en("ReceiptDelivery.EMAIL", "Email");
        fr("ReceiptDelivery.EMAIL", "Courriel");
        en("ReceiptDelivery.BOTH", "App and email");
        fr("ReceiptDelivery.BOTH", "Application et courriel");
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
        return List.of(Locale.ENGLISH, FRENCH);
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(Locale language) {
        Objects.requireNonNull(language);
        if (!byLanguage.containsKey(language.getLanguage())) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        this.language = language;
    }

    /** Every key defined for {@code locale}'s language; empty if the language is not supported. */
    public Set<String> keys(Locale locale) {
        return Set.copyOf(byLanguage.getOrDefault(locale.getLanguage(), Map.of()).keySet());
    }

    /** Null when the language or the key is missing. */
    private String lookup(String language, String key) {
        return byLanguage.getOrDefault(language, Map.of()).get(key);
    }

    private void en(String key, String text) {
        put(Locale.ENGLISH, key, text);
    }

    private void fr(String key, String text) {
        put(FRENCH, key, text);
    }

    private void put(Locale locale, String key, String text) {
        byLanguage.computeIfAbsent(locale.getLanguage(), l -> new HashMap<>()).put(key, text);
    }
}
