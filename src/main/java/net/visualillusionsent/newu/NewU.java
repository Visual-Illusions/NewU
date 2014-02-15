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

import net.canarymod.commandsys.CommandDependencyException;
import net.visualillusionsent.minecraft.plugin.canary.VisualIllusionsCanaryPlugin;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Jason (darkdiplomat)
 */
public final class NewU extends VisualIllusionsCanaryPlugin {
    StationTracker tracker;

    @Override
    public boolean enable() {
        super.enable();
        try {
            tracker = new StationTracker();
            new RespawnStationListener(this);
            return true;
        }
        catch (CommandDependencyException cdex) {
            getPluginLogger().log(Level.SEVERE, "NewU failed to start...", cdex);
        }
        return false;
    }

    @Override
    public void disable() {
        try {
            tracker.storeStations();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
