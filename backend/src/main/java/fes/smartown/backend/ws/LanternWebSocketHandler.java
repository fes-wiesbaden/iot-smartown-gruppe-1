package fes.smartown.backend.ws;

import fes.smartown.backend.lanterns.service.LanternRealtimeService;
import fes.smartown.backend.lanterns.service.LanternStateService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
/**
 * Schiebt Snapshot-Aenderungen an Frontend-Clients, nimmt selbst aber keine Commands an.
 */
public class LanternWebSocketHandler extends TextWebSocketHandler {

    private final LanternRealtimeService lanternRealtimeService;
    private final LanternStateService lanternStateService;

    public LanternWebSocketHandler(LanternRealtimeService lanternRealtimeService,
                                   LanternStateService lanternStateService) {
        this.lanternRealtimeService = lanternRealtimeService;
        this.lanternStateService = lanternStateService;
    }

    @Override
    /**
     * Registriert neue Clients und liefert sofort den aktuellen Snapshot aus.
     */
    public void afterConnectionEstablished(WebSocketSession session) {
        lanternRealtimeService.register(session);
        lanternRealtimeService.sendSnapshot(session, lanternStateService.getSnapshot());
    }

    @Override
    /**
     * Ignoriert eingehende Nachrichten, weil Befehle bewusst nur ueber REST laufen.
     */
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    @Override
    /**
     * Raeumt geschlossene Sessions aus dem Broadcast-Register auf.
     */
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        lanternRealtimeService.unregister(session);
    }
}
