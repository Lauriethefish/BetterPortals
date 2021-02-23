package implementations;

import com.google.inject.Injector;
import com.lauriethefish.betterportals.bukkit.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class TestConfigHandler {
    public static void prepareConfig(Injector injector) {
        prepareConfig("defaultConfig.yml", injector);
    }

    public static void prepareConfig(String testConfigName, Injector injector) {
        FileConfiguration config = loadConfigFileFromResource(testConfigName);

        injector.getInstance(ConfigManager.class).loadValues(config, null);
    }

    private static FileConfiguration loadConfigFileFromResource(String name) {
        return YamlConfiguration.loadConfiguration(new File("src/test/resources/" + name));
    }
}
