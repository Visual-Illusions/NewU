/*
 * This file is part of NewU.
 *
 * Copyright © 2014-2014 Visual Illusions Entertainment
 *
 * NewU is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License v3 as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License v3 for more details.
 *
 * You should have received a copy of the GNU General Public License v3 along with this program.
 * If not, see http://www.gnu.org/licenses/gpl.html.
 */
package net.visualillusionsent.newu;

import net.canarymod.Canary;
import net.canarymod.api.chat.ChatComponent;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.world.position.Location;
import net.canarymod.api.world.position.Vector3D;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.commandsys.Command;
import net.canarymod.commandsys.CommandDependencyException;
import net.canarymod.hook.HookHandler;
import net.canarymod.hook.player.DisconnectionHook;
import net.canarymod.hook.player.PlayerMoveHook;
import net.canarymod.hook.player.PlayerRespawnedHook;
import net.canarymod.hook.player.PlayerRespawningHook;
import net.canarymod.plugin.PluginListener;
import net.canarymod.plugin.Priority;
import net.visualillusionsent.minecraft.plugin.ChatFormat;
import net.visualillusionsent.minecraft.plugin.canary.VisualIllusionsCanaryPluginInformationCommand;

import java.util.HashMap;

/**
 * @author Jason (darkdiplomat)
 */
public final class RespawnStationListener extends VisualIllusionsCanaryPluginInformationCommand implements PluginListener {
    private final String newU = ChatFormat.LIGHT_GRAY + "[" + ChatFormat.YELLOW + "NewU" + ChatFormat.LIGHT_GRAY + "] " + ChatFormat.CYAN + "%s";
    private final HashMap<Player, String[]> pending;
    private final HashMap<Player, Vector3D> cache;

    public RespawnStationListener(NewU newu) throws CommandDependencyException {
        super(newu);
        pending = new HashMap<Player, String[]>();
        cache = new HashMap<Player, Vector3D>();
        newu.registerListener(this);
        newu.registerCommands(this, false);
    }

    @Command(
            aliases = { "newu" },
            description = "NewU Information",
            permissions = { "" },
            toolTip = "/newu [set <name>|del]",
            version = 2
    )
    public final void newu(MessageReceiver receiver, String[] args) {
        this.sendInformation(receiver);
    }

    @Command(
            aliases = { "set" },
            description = "Sets a new Station at current location",
            permissions = { "newu.set" },
            toolTip = "/newu set <name>",
            helpLookup = "newu set",
            parent = "newu",
            min = 1
    )
    public final void setNewU(MessageReceiver receiver, String[] args) {
        if (receiver instanceof Player) {
            if (NewU.tracker.addStation(new NewUStation(args[0], ((Player) receiver).getLocation()))) {
                receiver.message(String.format(newU, "Station Added!"));
            }
            else {
                receiver.notice("Failed to create new station... (Too close to another station?)");
            }
        }
        else {
            receiver.notice("Only Players may set up new NewU Stations currently");
        }
    }

    @Command(
            aliases = { "del" },
            description = "Deletes a NewU station",
            permissions = { "newu.del" },
            toolTip = "/newu del <name>",
            parent = "newu",
            min = 1
    )
    public final void delNewU(MessageReceiver receiver, String[] args) {
        if (NewU.tracker.removeStation(args[0])) {
            receiver.message(String.format(newU, "Station " + args[0] + " removed."));
        }
        else {
            receiver.notice("Station not found...");
        }
    }

    @HookHandler(priority = Priority.PASSIVE) // Cause, you know, being last makes us the only thing
    public final void respawn(PlayerRespawningHook hook) {
        Player player = hook.getPlayer();
        if (player.hasPermission("newu.use")) {
            if (NewU.cfg.isCharging()) {
                if (TransactionHandler.hasAmount(player)) {
                    pending.put(player, new String[]{ NewU.tracker.getRandomMessage(), String.format("Fee: %.2f", TransactionHandler.percentage(player)) });
                }
                else if (NewU.cfg.isWaivable()) {
                    pending.put(player, new String[]{ NewU.tracker.getRandomMessage(), "Fee: Waived" });
                }
                else {
                    return; // Not Waivable and no money? Do not pass go, do not collect $200
                }
            }
            else {
                pending.put(player, new String[]{ NewU.tracker.getRandomMessage(), "Fee: None" });
            }
            cache.remove(player);
            Location respawn = NewU.tracker.getClosestRespawn(player);
            if (respawn != null) {
                hook.setRespawnLocation(respawn);
            }
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public final void respawned(PlayerRespawnedHook hook) {
        Player player = hook.getPlayer();
        if (pending.containsKey(player)) {
            TransactionHandler.charge(player);
            String[] msgs = pending.remove(player);
            player.message(String.format(newU, msgs[0]));
            player.message(String.format(newU, msgs[1]));
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public final void nearStation(PlayerMoveHook hook) {
        Player player = hook.getPlayer();
        if (!cache.containsKey(player) || cache.get(player).getDistance(hook.getTo()) > 10) {
            cache.put(player, hook.getTo());
            NewUStation closest = NewU.tracker.getClosestStation(player);
            if (closest != null && !closest.hasDiscoveredNoAdd(player)) {
                closest.addDiscoverer(player.getName());
                ChatComponent title = Canary.factory().getChatComponentFactory().newChatComponent(closest.getName() + " Discovered");
                ChatComponent subtitle = Canary.factory().getChatComponentFactory().newChatComponent("A NewU Station");
                title.getChatStyle().setColor(Canary.factory().getChatComponentFactory().colorAqua());
                subtitle.getChatStyle().setColor(Canary.factory().getChatComponentFactory().colorYellow());
                player.showTitle(title, subtitle);
                player.message(String.format(newU, "You have discovered NewU Station: '" + closest.getName() + "'."));
            }
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public final void disconnected(DisconnectionHook hook) {
        cache.remove(hook.getPlayer());
        pending.remove(hook.getPlayer());
    }
}
