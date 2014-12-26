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

import net.visualillusionsent.utils.PropertiesFile;

/**
 * @author Jason (darkdiplomat)
 */
final class NewUConfiguration {
    private final PropertiesFile cfg;

    NewUConfiguration(NewU newU) {
        cfg = new PropertiesFile(NewU.cfgDir.getAbsolutePath().concat("/settings.cfg"));

        cfg.getBoolean("respawn.charge", false);
        cfg.setComments("respawn.charge", "Whether to charge for use of a NewU station");
        cfg.getDouble("charge.percent", 7.0D);
        cfg.setComments("charge.percent", "A percentage of the Player's current monetary holdings, not to exceed 50% and not less than 1%");
        cfg.getBoolean("waive.payments", true);
        cfg.setComments("waive.payments", "Whether to allow use of a NewU station if not able to pay");
        cfg.save();
    }

    public final boolean isCharging() {
        return cfg.getBoolean("respawn.charge");
    }

    public final double chargePercent() {
        return cfg.getDouble("charge.percent") / 100;
    }

    public final boolean isWaivable() {
        return cfg.getBoolean("waive.payments");
    }
}
