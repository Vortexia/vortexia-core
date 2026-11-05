// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.item;

import me.alikuxac.vortexia.api.model.Identity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CitizenCardMapRenderer extends MapRenderer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private final Identity identity;
    private final Set<UUID> renderedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private BufferedImage cachedImage;
    private BufferedImage skinAvatar;

    public CitizenCardMapRenderer(Identity identity) {
        super(false);
        this.identity = identity;
        this.cachedImage = renderCardImage();
        loadSkinAsync();
    }

    private void loadSkinAsync() {
        if (identity == null || identity.getUuid() == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Player player = Bukkit.getPlayer(identity.getUuid());
                URL skinUrl = null;
                if (player != null && player.getPlayerProfile() != null && player.getPlayerProfile().getTextures() != null) {
                    skinUrl = player.getPlayerProfile().getTextures().getSkin();
                }
                if (skinUrl == null) {
                    skinUrl = URI.create("https://mc-heads.net/avatar/" + identity.getUuid() + "/32").toURL();
                }
                try (InputStream in = skinUrl.openStream()) {
                    BufferedImage downloaded = ImageIO.read(in);
                    if (downloaded != null) {
                        BufferedImage head;
                        if (downloaded.getWidth() == 32 && downloaded.getHeight() == 32) {
                            head = downloaded;
                        } else {
                            head = extractHead(downloaded);
                        }
                        if (head != null) {
                            this.skinAvatar = head;
                            this.cachedImage = renderCardImage();
                            this.renderedPlayers.clear();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private BufferedImage extractHead(BufferedImage skin) {
        if (skin.getWidth() < 16 || skin.getHeight() < 16) return null;
        BufferedImage head = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = head.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        BufferedImage face = skin.getSubimage(8, 8, 8, 8);
        g.drawImage(face, 0, 0, 32, 32, null);
        if (skin.getWidth() >= 48 && skin.getHeight() >= 16) {
            BufferedImage hat = skin.getSubimage(40, 8, 8, 8);
            g.drawImage(hat, 0, 0, 32, 32, null);
        }
        g.dispose();
        return head;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (player == null || cachedImage == null) {
            return;
        }

        if (renderedPlayers.add(player.getUniqueId())) {
            canvas.drawImage(0, 0, cachedImage);
        }
    }

    private BufferedImage renderCardImage() {
        BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setColor(new Color(16, 22, 32));
        g.fillRect(0, 0, 128, 128);

        g.setColor(new Color(22, 32, 46));
        for (int y = 22; y < 116; y += 8) {
            g.drawLine(3, y, 124, y);
        }
        for (int x = 6; x < 124; x += 16) {
            g.drawLine(x, 22, x, 116);
        }

        g.setColor(new Color(0, 200, 240));
        g.drawRect(2, 2, 123, 123);
        g.setColor(new Color(0, 80, 120));
        g.drawRect(3, 3, 121, 121);

        g.setColor(new Color(0, 255, 255));
        g.fillRect(1, 1, 6, 2);
        g.fillRect(1, 1, 2, 6);
        g.fillRect(121, 1, 6, 2);
        g.fillRect(125, 1, 2, 6);
        g.fillRect(1, 125, 6, 2);
        g.fillRect(1, 121, 2, 6);
        g.fillRect(121, 125, 6, 2);
        g.fillRect(125, 121, 2, 6);

        g.setColor(new Color(10, 36, 60));
        g.fillRect(4, 4, 120, 17);
        g.setColor(new Color(0, 200, 240));
        g.drawLine(4, 21, 123, 21);

        g.setColor(new Color(0, 255, 255));
        g.fillRect(7, 10, 3, 3);
        g.fillRect(8, 9, 1, 5);
        g.fillRect(6, 11, 5, 1);

        drawMinecraftText(g, "VORTEXIA CITIZEN", 15, 6, Color.WHITE);
        drawMinecraftText(g, "IDENTITY CARD", 15, 13, new Color(0, 220, 255));
        drawMinecraftText(g, "VX", 110, 9, new Color(255, 215, 0));

        int px = 7, py = 25, pw = 34, ph = 34;
        g.setColor(new Color(6, 12, 18));
        g.fillRect(px, py, pw, ph);
        g.setColor(new Color(0, 200, 240));
        g.drawRect(px, py, pw, ph);

        if (skinAvatar != null) {
            g.drawImage(skinAvatar, px + 1, py + 1, 32, 32, null);
        } else {
            int fx = px + 1, fy = py + 1;
            g.setColor(new Color(45, 28, 15));
            g.fillRect(fx + 4, fy + 2, 24, 7);
            g.setColor(new Color(212, 160, 125));
            g.fillRect(fx + 4, fy + 9, 24, 15);
            g.setColor(Color.WHITE);
            g.fillRect(fx + 6, fy + 14, 4, 3);
            g.fillRect(fx + 18, fy + 14, 4, 3);
            g.setColor(new Color(45, 90, 160));
            g.fillRect(fx + 8, fy + 14, 2, 3);
            g.fillRect(fx + 18, fy + 14, 2, 3);
            g.setColor(new Color(110, 60, 40));
            g.fillRect(fx + 12, fy + 20, 8, 2);
            g.setColor(new Color(0, 140, 160));
            g.fillRect(fx + 2, fy + 24, 28, 8);
        }

        int cx = 11, cy = 63, cw = 26, ch = 17;
        g.setColor(new Color(220, 180, 50));
        g.fillRect(cx, cy, cw, ch);
        g.setColor(new Color(130, 95, 20));
        g.drawRect(cx, cy, cw - 1, ch - 1);
        g.drawLine(cx + 7, cy, cx + 7, cy + ch - 1);
        g.drawLine(cx + cw - 8, cy, cx + cw - 8, cy + ch - 1);
        g.drawLine(cx, cy + 8, cx + cw - 1, cy + 8);
        g.setColor(new Color(255, 235, 120));
        g.fillRect(cx + 9, cy + 4, cw - 18, ch - 8);

        int qx = 13, qy = 85;
        g.setColor(Color.WHITE);
        g.fillRect(qx, qy, 22, 22);
        g.setColor(Color.BLACK);
        g.fillRect(qx + 1, qy + 1, 6, 6);
        g.setColor(Color.WHITE);
        g.fillRect(qx + 2, qy + 2, 4, 4);
        g.setColor(Color.BLACK);
        g.fillRect(qx + 3, qy + 3, 2, 2);
        g.fillRect(qx + 15, qy + 1, 6, 6);
        g.setColor(Color.WHITE);
        g.fillRect(qx + 16, qy + 2, 4, 4);
        g.setColor(Color.BLACK);
        g.fillRect(qx + 17, qy + 3, 2, 2);
        g.fillRect(qx + 1, qy + 15, 6, 6);
        g.setColor(Color.WHITE);
        g.fillRect(qx + 2, qy + 16, 4, 4);
        g.setColor(Color.BLACK);
        g.fillRect(qx + 3, qy + 17, 2, 2);
        g.fillRect(qx + 9, qy + 3, 3, 2);
        g.fillRect(qx + 8, qy + 8, 3, 3);
        g.fillRect(qx + 13, qy + 9, 2, 3);
        g.fillRect(qx + 9, qy + 15, 3, 2);
        g.fillRect(qx + 15, qy + 14, 2, 3);
        g.fillRect(qx + 18, qy + 18, 2, 2);

        int dx = 46;
        String name = identity != null && identity.getName() != null ? identity.getName() : "Unknown";
        if (name.length() > 11) name = name.substring(0, 10) + "..";
        drawMinecraftText(g, "NAME", dx, 25, new Color(130, 150, 175));
        drawMinecraftText(g, name, dx, 33, Color.WHITE);

        String cid = identity != null && identity.getCitizenId() != null ? identity.getCitizenId() : "N/A";
        drawMinecraftText(g, "CID", dx, 43, new Color(130, 150, 175));
        drawMinecraftText(g, cid, dx, 51, new Color(255, 215, 0));

        boolean isPremium = identity != null && identity.hasPremiumUuid();
        drawMinecraftText(g, "RANK", dx, 61, new Color(130, 150, 175));
        drawMinecraftText(g, isPremium ? "PREMIUM" : "STANDARD", dx, 69, isPremium ? new Color(255, 175, 0) : Color.LIGHT_GRAY);
        if (isPremium) {
            int sx = dx + 44, sy = 69;
            g.setColor(new Color(255, 215, 0));
            g.fillRect(sx + 2, sy, 1, 5);
            g.fillRect(sx, sy + 2, 5, 1);
            g.fillRect(sx + 1, sy + 1, 3, 3);
        }

        boolean isVerified = identity != null && identity.getPin() != null && !identity.getPin().isEmpty();
        drawMinecraftText(g, "STATUS", dx, 79, new Color(130, 150, 175));
        if (isVerified) {
            g.setColor(new Color(85, 255, 85));
            g.drawLine(dx, 90, dx + 2, 92);
            g.drawLine(dx + 2, 92, dx + 6, 87);
            drawMinecraftText(g, "VERIFIED", dx + 8, 87, new Color(85, 255, 85));
        } else {
            drawMinecraftText(g, "UNSECURED", dx, 87, new Color(255, 85, 85));
        }

        long createdAt = identity != null ? identity.getCreatedAt() : System.currentTimeMillis();
        String issuedDate = DATE_FORMATTER.format(Instant.ofEpochMilli(createdAt));
        drawMinecraftText(g, "ISSUED", dx, 97, new Color(130, 150, 175));
        drawMinecraftText(g, issuedDate, dx, 105, Color.WHITE);

        g.setColor(new Color(8, 20, 36));
        g.fillRect(4, 116, 120, 9);
        g.setColor(new Color(0, 190, 225));
        g.drawLine(4, 115, 123, 115);
        String mrz = "ID<VX<" + name.toUpperCase() + "<" + (cid.length() > 6 ? cid.substring(0, 6) : cid) + "<";
        drawMinecraftText(g, mrz, 6, 117, new Color(0, 220, 255));

        g.dispose();
        return image;
    }

    private void drawMinecraftText(Graphics2D g, String text, int startX, int startY, Color color) {
        if (text == null) return;
        g.setColor(color);
        int currX = startX;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            MapFont.CharacterSprite sprite = MinecraftFont.Font.getChar(ch);
            if (sprite == null) {
                currX += 4;
                continue;
            }
            for (int r = 0; r < sprite.getHeight(); r++) {
                for (int c = 0; c < sprite.getWidth(); c++) {
                    if (sprite.get(r, c)) {
                        g.fillRect(currX + c, startY + r, 1, 1);
                    }
                }
            }
            currX += sprite.getWidth() + 1;
        }
    }
}
