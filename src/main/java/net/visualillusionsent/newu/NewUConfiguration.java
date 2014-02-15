package net.visualillusionsent.newu;

import net.visualillusionsent.utils.PropertiesFile;

/**
 * @author Jason (darkdiplomat)
 */
final class NewUConfiguration {
    private final PropertiesFile cfg;

    NewUConfiguration(NewU newU) {
        cfg = new PropertiesFile(NewU.cfgDir.getAbsolutePath().concat("/settings.cfg"));
    }
}
