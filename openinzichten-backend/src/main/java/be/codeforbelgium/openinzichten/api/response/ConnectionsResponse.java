package be.codeforbelgium.openinzichten.api.response;

import java.util.List;

public record ConnectionsResponse(List<ConnectionAccountResponse> connections,
                                  List<ConnectionRequestResponse> requests) {
}
