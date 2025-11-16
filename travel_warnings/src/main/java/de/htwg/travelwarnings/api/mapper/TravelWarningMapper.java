package de.htwg.travelwarnings.api.mapper;

import de.htwg.travelwarnings.api.dto.TravelWarningDetailDto;
import de.htwg.travelwarnings.api.dto.TravelWarningDto;
import de.htwg.travelwarnings.api.dto.UserTripDto;
import de.htwg.travelwarnings.persistence.entity.TravelWarning;
import de.htwg.travelwarnings.persistence.entity.UserTrip;
import de.htwg.travelwarnings.service.WarningContentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Mapper for converting between domain models and DTOs
 */
@ApplicationScoped
public class TravelWarningMapper {

    @Inject
    WarningContentService contentService;

    public TravelWarningDto toDto(TravelWarning warning) {
        if (warning == null) return null;

        TravelWarningDto dto = new TravelWarningDto();
        dto.setContentId(warning.getContentId());
        dto.setLastModified(warning.getLastModified());
        dto.setEffective(warning.getEffective());
        dto.setTitle(warning.getTitle());
        dto.setCountryCode(warning.getCountryCode());
        dto.setIso3CountryCode(warning.getIso3CountryCode());
        dto.setCountryName(warning.getCountryName());
        dto.setWarning(warning.getWarning());
        dto.setPartialWarning(warning.getPartialWarning());
        dto.setSituationWarning(warning.getSituationWarning());
        dto.setSituationPartWarning(warning.getSituationPartWarning());
        dto.setSeverity(warning.getSeverity());
        dto.setSeverityDisplay(warning.getSeverity().getDisplayName());
        dto.setHasActiveWarning(warning.hasActiveWarning());
        return dto;
    }

    public TravelWarningDetailDto toDetailDto(TravelWarning warning) {
        if (warning == null) return null;

        TravelWarningDetailDto detailDto = new TravelWarningDetailDto();
        detailDto.setWarning(toDto(warning));
        detailDto.setContent(warning.getContent());

        // Categorize content
        WarningContentService.WarningCategories categories = contentService.categorizeContent(warning);
        detailDto.setCategories(TravelWarningDetailDto.ContentCategories.from(categories));

        // Generate official link
        detailDto.setOfficialLink("https://www.auswaertiges-amt.de/de/service/laender/" +
                                 warning.getCountryCode().toLowerCase() + "-sicherheit");

        return detailDto;
    }

    public UserTripDto toDto(UserTrip trip) {
        if (trip == null) return null;

        UserTripDto dto = new UserTripDto();
        dto.setId(trip.getId());
        dto.setEmail(trip.getEmail());
        dto.setCountryCode(trip.getCountryCode());
        dto.setCountryName(trip.getCountryName());
        dto.setStartDate(trip.getStartDate());
        dto.setEndDate(trip.getEndDate());
        dto.setTripName(trip.getTripName());
        dto.setNotificationsEnabled(trip.getNotificationsEnabled());
        return dto;
    }

    public UserTrip toEntity(UserTripDto dto) {
        if (dto == null) return null;

        UserTrip trip = new UserTrip();
        trip.setId(dto.getId());
        trip.setEmail(dto.getEmail());
        trip.setCountryCode(dto.getCountryCode());
        trip.setCountryName(dto.getCountryName());
        trip.setStartDate(dto.getStartDate());
        trip.setEndDate(dto.getEndDate());
        trip.setTripName(dto.getTripName());
        trip.setNotificationsEnabled(dto.getNotificationsEnabled() != null ? dto.getNotificationsEnabled() : true);
        return trip;
    }
}

