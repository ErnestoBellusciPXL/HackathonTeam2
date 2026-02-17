package be.codeforbelgium.openinzichten.api.response;

public record MunicipalityCommunitymemberCountResponse(
        String municipality,
        String municipalityCode,
        long amountOfCommunitymembers) {
}
