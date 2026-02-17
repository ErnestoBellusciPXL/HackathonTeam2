package be.codeforbelgium.openinzichten.api.response;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountStatusResponseTests {

    @Test
    void record_getters_and_values() {
        Instant now = Instant.now();
        AccountStatusResponse r = new AccountStatusResponse("id1", "user", true, "msg", "reason", now);

        assertThat(r.id()).isEqualTo("id1");
        assertThat(r.username()).isEqualTo("user");
        assertThat(r.disabled()).isTrue();
        assertThat(r.message()).isEqualTo("msg");
        assertThat(r.disabledReason()).isEqualTo("reason");
        assertThat(r.disabledAt()).isEqualTo(now);
    }
}
