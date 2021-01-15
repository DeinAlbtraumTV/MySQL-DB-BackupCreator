package config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public final class ConfigController {


    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File config_file = new File("./BackupCreator_config.cfg");
    public static Config cfg;
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);

    /**
     * (Re)loads the config file
     */
    public static void loadConfig() {
        try {
            if (!config_file.exists()) {
                cfg = new Config();
                Writer w = new FileWriter(config_file);
                gson.toJson(cfg, w);
                w.close();
                logger.info("Config created, please enter your values and start the programm again!");
                System.exit(0);
            }
            final JsonReader r = new JsonReader(new FileReader(config_file));
            cfg = gson.fromJson(r, Config.class);
            if (Config.CURRENT_CFG_VERSION != cfg.cfgVersion) {
                saveConfig();
            }
            r.close();
        } catch (IOException e) {
            System.err.println("FAILED TO SAVE/LOAD CONFIG");
            e.printStackTrace();
        }
    }

    /**
     * Saves changed entries to config file
     */
    public static void saveConfig() throws IOException {
        cfg.cfgVersion = Config.CURRENT_CFG_VERSION;
        Writer w = new FileWriter(config_file);
        gson.toJson(cfg, w);
        w.close();
    }
}
