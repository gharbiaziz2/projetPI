package tn.esprit.services;

/**
 * Thrown when a country name is not found via the RestCountries API.
 */
public class CountryNotFoundException extends RuntimeException {
    public CountryNotFoundException(String countryName) {
        super("Pays introuvable : \"" + countryName + "\". Vérifiez l'orthographe (en anglais).");
    }
}
