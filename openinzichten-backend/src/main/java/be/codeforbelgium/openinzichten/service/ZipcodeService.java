package be.codeforbelgium.openinzichten.service;

import be.codeforbelgium.openinzichten.api.response.ZipcodeResponse;
import be.codeforbelgium.openinzichten.domain.Zipcode;
import be.codeforbelgium.openinzichten.repository.ZipcodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ZipcodeService {

    private final ZipcodeRepository repo;

    public ZipcodeService(ZipcodeRepository repo) {
        this.repo = repo;
    }

    public List<String> findGemeentenByZipcode(String zipcode) {
        return repo.findAllByCode(zipcode)
                .stream()
                .map(Zipcode::getGemeente)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public List<ZipcodeResponse> findAllZipcodes() {
        return repo.findAll()
                .stream()
                .map(z -> new ZipcodeResponse(z.getCode(), z.getGemeente()))
                .toList();
    }
}
