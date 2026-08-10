// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.proxy;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Optional;
import java.util.UUID;

public class ProxyMessageDecoder {

    public record AuthSyncPayload(UUID uuid) {}

    public static Optional<AuthSyncPayload> parseAuthSync(byte[] data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             DataInputStream in = new DataInputStream(bis)) {
            String subChannel = in.readUTF();
            if ("AUTH_SYNC".equals(subChannel)) {
                String uuidStr = in.readUTF();
                return Optional.of(new AuthSyncPayload(UUID.fromString(uuidStr)));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
