package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.ZipcodeResponse;
import be.codeforbelgium.openinzichten.service.ZipcodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/zipcodes")
public class ZipcodeController {

    private final ZipcodeService service;

    public ZipcodeController(ZipcodeService service) {
        this.service = service;
    }

    @GetMapping("/{zipcode}/gemeenten")
    public ResponseEntity<List<String>> getGemeenten(@PathVariable String zipcode) {
        List<String> gemeenten = service.findGemeentenByZipcode(zipcode);
        if (gemeenten == null || gemeenten.isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(gemeenten);
    }

    @GetMapping
    public ResponseEntity<List<ZipcodeResponse>> getAllZipcodes() {
        List<ZipcodeResponse> all = service.findAllZipcodes();
        return ResponseEntity.ok(all);
    }
}
