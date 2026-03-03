package tn.esprit.dto;

import java.util.List;

/**
 * Simplified DTO for a country from the RestCountries API.
 * Only contains the fields we need for display.
 */
public record CountryInfoDto(
                String commonName,
                String capital,
                String currencyName,
                String currencySymbol,
                List<String> languages,
                String flagUrl,
                List<String> timezones,
                String googleMapsUrl) {
}
