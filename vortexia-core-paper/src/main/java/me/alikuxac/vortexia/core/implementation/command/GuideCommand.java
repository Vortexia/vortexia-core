// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.command;

import dev.jorel.commandapi.CommandAPICommand;
import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;

public class GuideCommand implements SubCommand {

    private final VortexiaCore plugin;

    public GuideCommand(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandAPICommand getSubcommandBuilder() {
        return new CommandAPICommand("guide")
                .withAliases("creative", "guidebook")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cOnly players can use this command!");
                        return;
                    }
                    plugin.getGuideGUI().openMain(player);
                });
    }
}
