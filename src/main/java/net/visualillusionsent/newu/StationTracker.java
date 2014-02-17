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
import com.google.gson.stream.JsonWriter;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.world.position.Location;
import net.visualillusionsent.utils.JarUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

/**
 * @author Jason (darkdiplomat)
 */
final class StationTracker {
    private final Random randy = new Random();
    private final List<NewUStation> stations = Collections.synchronizedList(new ArrayList<NewUStation>());
    private final List<String> respawnMessages;
    private final Logger logger;

    StationTracker(NewU newU) {
        logger = newU.getPluginLogger();
        loadStations();
        respawnMessages = Collections.unmodifiableList(loadMessages());
    }

    final boolean addStation(NewUStation station) {
        // Check that we are beyond 50 blocks from another station
        synchronized (stations) {
            for (NewUStation preExisting : stations) {
                if (preExisting.getStationLocation().getDistance(station.getStationLocation()) < 50) {
                    return false;
                }
            }
        }
        stations.add(station);
        storeStations();
        return true;
    }

    final void removeStation(NewUStation station) {
        stations.remove(station);
        storeStations();
    }

    Location getClosestRespawn(Player player) {
        double distance = -1;
        NewUStation going = null;

        synchronized (stations) {
            for (NewUStation station : stations) {
                if (station.hasDiscovered(player)) {
                    double dist = station.distanceFrom(player);
                    if (distance == -1) {
                        distance = dist;
                        going = station;
                    }
                    else if (dist < distance) {
                        distance = dist;
                        going = station;
                    }
                }
            }
        }
        return going != null ? going.getRespawnLocation() : player.getWorld().getSpawnLocation();
    }

    NewUStation getClosestStation(Player player) {
        double distance = -1;
        NewUStation closest = null;

        synchronized (stations) {
            for (NewUStation station : stations) {
                double dist = station.distanceFrom(player);
                if (distance == -1) {
                    distance = dist;
                    closest = station;
                }
                else if (dist < distance) {
                    distance = dist;
                    closest = station;
                }
            }
        }
        return closest;
    }

    final String getRandomMessage() {
        return respawnMessages.get(randy.nextInt(respawnMessages.size()));
    }

    private ArrayList<String> loadMessages() {
        ArrayList<String> temp = new ArrayList<String>();
        Scanner scanner = null;
        try {
            JarFile disJar = new JarFile(JarUtils.getJarPath(getClass()));
            ZipEntry entry = disJar.getEntry("resources/lang/en_US.lang");
            scanner = new Scanner(disJar.getInputStream(entry));
            while (scanner.hasNextLine()) {
                temp.add(scanner.nextLine());
            }
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to load reconstruction messages...");
        }
        finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return temp;
    }

    final boolean storeStations() {
        boolean failure = false;
        File stationsJSON = new File(NewU.cfgDir, "stations.json.tmp"); //Store to temp first
        JsonWriter writer = null;
        PrintWriter pWriter = null;
        try {
            stationsJSON.createNewFile();
            pWriter = new PrintWriter(stationsJSON);
            writer = new JsonWriter(pWriter);
            writer.beginObject(); // Master Object
            pWriter.println();
            for (NewUStation station : stations) {
                pWriter.print("\t");
                writer.name("Station");
                writer.beginObject(); // Station object
                writer.name("Location");
                writer.beginObject(); // Location Object
                writer.name("World");
                Location loc = station.getStationLocation();
                writer.value(loc.getWorldName());
                writer.name("Dimension");
                writer.value(loc.getType().getName());
                writer.name("X");
                writer.value(loc.getBlockX());
                writer.name("Y");
                writer.value(loc.getBlockY());
                writer.name("Z");
                writer.value(loc.getBlockZ());
                writer.endObject(); // End Location
                writer.name("Discoverers");
                writer.beginArray(); // Discoverers Array
                for (String discoverer : station.discoverers()) {
                    writer.value(discoverer);
                }
                writer.endArray(); // End Discoverers
                writer.endObject(); // End Station
                pWriter.println();
            }
            writer.endObject(); // End Master
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to store stations...");
            failure = true;
        }
        finally {
            try {
                if (writer != null)
                    writer.close();
            }
            catch (IOException ioex) {
                //IGNORED
            }
            if (pWriter != null)
                pWriter.close();
        }
        if (!failure) {
            failure = !stationsJSON.renameTo(new File(NewU.cfgDir, "stations.json"));
        }

        return !failure;
    }

    private void loadStations() {
        try {
            File stationsJSON = new File(NewU.cfgDir, "stations.json");
            if (!stationsJSON.exists()) {
                stationsJSON.createNewFile();
                return;
            }

            JsonReader reader = new JsonReader(new FileReader(stationsJSON));
            reader.beginObject(); // Begin main object
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("Station")) {
                    NewUStation temp = null;
                    reader.beginObject(); // Begin Station
                    while (reader.hasNext()) {
                        name = reader.nextName();
                        if (name.equals("Location")) {
                            reader.beginObject(); // Begin Location
                            temp = new NewUStation(reader); // Pass reader into NewUStation object for parsing
                            reader.endObject(); // End Location
                        }
                        else if (name.equals("Discoverers")) {
                            reader.beginArray(); // Begin Discoverers
                            while (reader.hasNext()) {
                                if (temp != null) {
                                    temp.addDiscoverer(reader.nextString());
                                }
                            }
                            reader.endArray(); // End Discoverers
                        }
                        else {
                            reader.skipValue(); // UNKNOWN THING
                        }
                    }
                    if (temp != null) {
                        stations.add(temp);
                    }
                    reader.endObject(); //End Station
                }
            }

            reader.endObject(); // End main object
            reader.close();
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load stations...");
        }
    }
}
