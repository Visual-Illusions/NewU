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

import com.google.gson.stream.JsonReader;
import net.canarymod.ToolBox;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.world.DimensionType;
import net.canarymod.api.world.position.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jason (darkdiplomat)
 */
final class NewUStation {
    private static final String jsonForm = "\"Station\":{\"Location\":{\"World\":\"%s\",\"Dimension\":\"%s\",\"X\":%.2f,\"Y\":%.2f,\"Z\":%.2f,\"RotX\":%.2f,\"RotY\":%.2f},\"Discoverers\":[%s]}";
    private final Location station;
    private final List<String> discoverers;

    NewUStation(Location station) {
        this.discoverers = Collections.synchronizedList(new ArrayList<String>());
        // Adjust for centering
        this.station = new Location(station.getBlockX() + 0.5, station.getBlockY() + 0.1, station.getBlockZ() + 0.5);
        this.station.setWorldName(station.getWorldName());
        this.station.setType(station.getType());
    }

    NewUStation(JsonReader reader) throws IOException {
        this.discoverers = Collections.synchronizedList(new ArrayList<String>());

        this.station = new Location(0, 0, 0); // Initialize
        while (reader.hasNext()) {
            String name = reader.nextName();

            if (name.equals("World")) {
                station.setWorldName(reader.nextString());
            }
            else if (name.equals("Dimension")) {
                station.setType(DimensionType.fromName(reader.nextString()));
            }
            else if (name.equals("X")) {
                station.setX(reader.nextDouble());
            }
            else if (name.equals("Y")) {
                station.setY(reader.nextDouble());
            }
            else if (name.equals("Z")) {
                station.setZ(reader.nextDouble());
            }
            else if (name.equals("RotX")) {
                station.setRotation((float) reader.nextDouble());
            }
            else if (name.equals("RotY")) {
                station.setPitch((float) reader.nextDouble());
            }
            else {
                reader.skipValue(); // Unknown
            }
        }
    }

    final void addDiscoverer(String name) {
        synchronized (discoverers) {
            discoverers.add(name);
        }
    }

    final boolean hasDiscovered(Player player) {
        synchronized (discoverers) {
            return discoverers.contains(player.getName()) || (distanceFrom(player) <= 10 && discoverers.add(player.getName()));
        }
    }

    final boolean hasDiscoveredNoAdd(Player player) {
        synchronized (discoverers) {
            return discoverers.contains(player.getName());
        }
    }

    final double distanceFrom(Player player) {
        if (!player.getWorld().getName().equals(station.getWorldName()) || player.getWorld().getType() != station.getType()) {
            return Double.MAX_VALUE;
        }
        return player.getLocation().getDistance(station);
    }

    final Location getStationLocation() {
        return station;
    }

    final Location getRespawnLocation() {
        Location temp = station.clone();
        double meh = Math.random();
        int adj = 2;
        if (meh > 0.3 && meh < 0.7) {
            adj = ToolBox.floorToBlock(meh * 10.0D);
        }
        temp.setX(station.getBlockX() + adj + 0.5);
        meh = Math.random();
        adj = 2;
        if (meh > 0.3 && meh < 0.7) {
            adj = ToolBox.floorToBlock(meh * 10.0D);
        }
        temp.setZ(station.getBlockZ() + adj + 0.5);
        return temp;
    }

    final String toJSON() {
        return String.format(jsonForm, station.getWorldName(), station.getType().getName(), station.getX(), station.getY(), station.getZ(), station.getRotation(), station.getPitch(), discoverers());
    }

    final String coordinates() {
        return String.format("X:%.2f;Y:%.2f;Z:%.2f", station.getX(), station.getY(), station.getZ());
    }

    @Override
    public final String toString() {
        return super.toString(); //TODO
    }

    private String discoverers() {
        synchronized (discoverers) {
            StringBuilder builder = new StringBuilder();
            for (String discoverer : discoverers) {
                builder.append("\"").append(discoverer).append("\",");
            }
            if (builder.length() > 1) {
                builder.deleteCharAt(builder.length() - 1); // Remove last Comma if present
            }
            return builder.toString();
        }
    }
}
