package be.codeforbelgium.openinzichten.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Test
    void configureMessageBroker_setsPrefixesAndBroker() {
        MessageBrokerRegistry brokerRegistry = mock(MessageBrokerRegistry.class);

        WebSocketConfig cfg = new WebSocketConfig(null);
        cfg.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(brokerRegistry).enableSimpleBroker("/topic", "/queue");
    }

    @Test
    void registerStompEndpoints_registersEndpoint_withSockJsAndAllowAllOrigins() {
        StompEndpointRegistry endpointRegistry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration sockJsRegistration = mock(SockJsServiceRegistration.class);

        when(endpointRegistry.addEndpoint("/api/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("*")).thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsRegistration);

        WebSocketConfig cfg = new WebSocketConfig(null);
        cfg.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistry).addEndpoint("/api/ws");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).withSockJS();
    }

    @Test
    void configureClientInboundChannel_withInterceptor_addsInterceptor() throws Exception {
        ChannelRegistration channelRegistration = mock(ChannelRegistration.class);

        WebSocketConfig cfg = new WebSocketConfig(null);
        be.codeforbelgium.openinzichten.config.StompAuthChannelInterceptor interceptor =
                mock(be.codeforbelgium.openinzichten.config.StompAuthChannelInterceptor.class);

        // inject private field
        java.lang.reflect.Field f = WebSocketConfig.class.getDeclaredField("authInterceptor");
        f.setAccessible(true);
        f.set(cfg, interceptor);

        cfg.configureClientInboundChannel(channelRegistration);

        verify(channelRegistration).interceptors(interceptor);
    }

    @Test
    void configureClientInboundChannel_withoutInterceptor_noInteraction() throws Exception {
        ChannelRegistration channelRegistration = mock(ChannelRegistration.class);

        WebSocketConfig cfg = new WebSocketConfig(null);
        // ensure interceptor is null
        java.lang.reflect.Field f = WebSocketConfig.class.getDeclaredField("authInterceptor");
        f.setAccessible(true);
        f.set(cfg, null);

        cfg.configureClientInboundChannel(channelRegistration);

        verifyNoInteractions(channelRegistration);
    }
}
