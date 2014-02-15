/*
 * This file is part of NewU.
 *
 * Copyright © 2014 Visual Illusions Entertainment
 *
 * NewU is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/gpl.html.
 */
package net.visualillusionsent.newu;

import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.world.position.Vector3D;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.chat.TextFormat;
import net.canarymod.commandsys.Command;
import net.canarymod.commandsys.CommandDependencyException;
import net.canarymod.hook.HookHandler;
import net.canarymod.hook.player.DisconnectionHook;
import net.canarymod.hook.player.PlayerMoveHook;
import net.canarymod.hook.player.PlayerRespawnedHook;
import net.canarymod.hook.player.PlayerRespawningHook;
import net.canarymod.plugin.PluginListener;
import net.canarymod.plugin.Priority;
import net.visualillusionsent.minecraft.plugin.canary.VisualIllusionsCanaryPluginInformationCommand;

import java.util.HashMap;

/**
 * @author Jason (darkdiplomat)
 */
public final class RespawnStationListener extends VisualIllusionsCanaryPluginInformationCommand implements PluginListener {
    private final String newU = TextFormat.LIGHT_GRAY + "[" + TextFormat.YELLOW + "NewU" + TextFormat.LIGHT_GRAY + "] " + TextFormat.CYAN + "%s";
    private final StationTracker tracker;
    private final HashMap<Player, String> pending;
    private final HashMap<Player, Vector3D> cache;

    public RespawnStationListener(NewU newu) throws CommandDependencyException {
        super(newu);
        tracker = newu.tracker;
        pending = new HashMap<Player, String>();
        cache = new HashMap<Player, Vector3D>();
        newu.registerListener(this);
        newu.registerCommands(this, false);
    }

    @Command(
            aliases = { "newu" },
            description = "NewU Information",
            permissions = { "" },
            toolTip = "/newu [set|del]"
    )
    public final void newu(MessageReceiver receiver, String[] args) {
        this.sendInformation(receiver);
    }

    @Command(
            aliases = { "set" },
            description = "Sets a new Station at current location",
            permissions = { "newu.set" },
            toolTip = "/newu set",
            parent = "newu"
    )
    public final void setNewU(MessageReceiver receiver, String[] args) {
        if (receiver instanceof Player) {
            if (tracker.addStation(new NewUStation(((Player) receiver).getLocation()))) {
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
            description = "Deletes the closest NewU station",
            permissions = { "newu.del" },
            toolTip = "/newu del",
            parent = "newu"
    )
    public final void delNewU(MessageReceiver receiver, String[] args) {
        if (receiver instanceof Player) {
            NewUStation station = tracker.getClosestStation((Player) receiver);
            tracker.removeStation(station);
            receiver.message(String.format(newU, "Station @ " + station.coordinates() + " removed."));
        }
        else {
            receiver.notice("Player required to delete a nearby station");
        }
    }

    @HookHandler(priority = Priority.PASSIVE) // Cause, you know, being last makes us the only thing
    public final void respawn(PlayerRespawningHook hook) {
        if (hook.getPlayer().hasPermission("newu.use")) {
            hook.setRespawnLocation(tracker.getClosestRespawn(hook.getPlayer()));
            pending.put(hook.getPlayer(), tracker.getRandomMessage());
            cache.remove(hook.getPlayer());
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public final void respawned(PlayerRespawnedHook hook) {
        if (pending.containsKey(hook.getPlayer())) {
            hook.getPlayer().message(String.format(newU, pending.remove(hook.getPlayer())));
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public final void nearStation(PlayerMoveHook hook) {
        if (!cache.containsKey(hook.getPlayer()) || cache.get(hook.getPlayer()).getDistance(hook.getTo()) > 10) {
            cache.put(hook.getPlayer(), hook.getTo());
            NewUStation closest = tracker.getClosestStation(hook.getPlayer());
            if (closest != null && !closest.hasDiscoveredNoAdd(hook.getPlayer())) {
                closest.addDiscoverer(hook.getPlayer().getName());
                hook.getPlayer().message(String.format(newU, "You have discovered a new NewU station."));
            }
        }
    }

    @HookHandler(priority = Priority.PASSIVE)
    public final void disconnected(DisconnectionHook hook) {
        cache.remove(hook.getPlayer());
    }
}