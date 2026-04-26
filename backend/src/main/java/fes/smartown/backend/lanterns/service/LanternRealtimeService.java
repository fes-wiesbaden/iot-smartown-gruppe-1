package fes.smartown.backend.lanterns.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fes.smartown.backend.lanterns.model.LanternSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * Verteilt Snapshot-Aenderungen an alle verbundenen WebSocket-Clients.
 */
public class LanternRealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LanternRealtimeService.class);

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public LanternRealtimeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Merkt sich eine neue WebSocket-Session fuer kuenftige Broadcasts.
     */
    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    /**
     * Entfernt eine WebSocket-Session, die geschlossen oder defekt ist.
     */
    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    /**
     * Sendet einen Snapshot an genau eine Session und raeumt defekte Verbindungen auf.
     */
    public void sendSnapshot(WebSocketSession session, LanternSnapshot snapshot) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(snapshot)));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Could not serialize lantern snapshot for websocket session {}", session.getId(), exception);
        } catch (IOException exception) {
            unregister(session);
            LOGGER.warn("Could not send lantern snapshot to websocket session {}", session.getId(), exception);
        }
    }

    /**
     * Broadcastet den aktuellen Snapshot an alle registrierten Sessions.
     */
    public void broadcast(LanternSnapshot snapshot) {
        sessions.forEach(session -> sendSnapshot(session, snapshot));
    }
}
