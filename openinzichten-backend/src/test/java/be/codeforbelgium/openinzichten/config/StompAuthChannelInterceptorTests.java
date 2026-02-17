package be.codeforbelgium.openinzichten.config;

import be.codeforbelgium.openinzichten.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompAuthChannelInterceptorTests {


    @Test
    void preSend_noToken_doesNotSetUser() {
        JwtService jwtService = mock(JwtService.class);
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtService);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        org.springframework.messaging.support.GenericMessage<?> message = new org.springframework.messaging.support.GenericMessage<>(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    void preSend_invalidToken_doesNotSetUser() {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.isTokenValid("badtoken")).thenReturn(false);
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtService);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer badtoken");
        org.springframework.messaging.support.GenericMessage<?> message = new org.springframework.messaging.support.GenericMessage<>(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    void preSend_nonConnectCommand_doesNothing() {
        JwtService jwtService = mock(JwtService.class);
        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtService);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        org.springframework.messaging.support.GenericMessage<?> message = new org.springframework.messaging.support.GenericMessage<>(new byte[0], accessor.getMessageHeaders());
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> result = interceptor.preSend(message, channel);
        assertSame(message, result);
    }


}
