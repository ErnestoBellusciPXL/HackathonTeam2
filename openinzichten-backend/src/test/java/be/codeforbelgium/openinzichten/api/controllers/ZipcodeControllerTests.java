package be.codeforbelgium.openinzichten.api.controllers;

import be.codeforbelgium.openinzichten.api.response.ZipcodeResponse;
import be.codeforbelgium.openinzichten.security.JwtService;
import be.codeforbelgium.openinzichten.service.AccountUserDetailsService;
import be.codeforbelgium.openinzichten.service.ZipcodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ZipcodeController.class)
@AutoConfigureMockMvc(addFilters = false)
class ZipcodeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ZipcodeService zipcodeService;

    // mock security beans so the WebMvc test slice doesn't attempt to create real security filters
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AccountUserDetailsService accountUserDetailsService;

    @Test
    void returnsGemeentenForZipcode() throws Exception {
        when(zipcodeService.findGemeentenByZipcode("1000"))
                .thenReturn(List.of("Brussel", "Elsene", "Sint-Joost-ten-Node"));

        mockMvc.perform(get("/api/zipcodes/1000/gemeenten"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Brussel"))
                .andExpect(jsonPath("$[1]").value("Elsene"));
    }

    @Test
    void returns404WhenNoGemeentenFound() throws Exception {
        when(zipcodeService.findGemeentenByZipcode("99999"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/zipcodes/99999/gemeenten"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsAllZipcodes() throws Exception {
        List<ZipcodeResponse> sample = List.of(
                new ZipcodeResponse("1000", "Brussel"),
                new ZipcodeResponse("2000", "Antwerpen")
        );

        when(zipcodeService.findAllZipcodes()).thenReturn(sample);

        mockMvc.perform(get("/api/zipcodes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("1000"))
                .andExpect(jsonPath("$[0].gemeente").value("Brussel"));
    }
}
