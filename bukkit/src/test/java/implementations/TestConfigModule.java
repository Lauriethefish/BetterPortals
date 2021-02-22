package implementations;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

// Binds config during testing
public class TestConfigModule extends AbstractModule {
    private final String testConfigName;

    public TestConfigModule(String testConfigName) {
        this.testConfigName = testConfigName;
    }

    public TestConfigModule() {
        this("defaultConfig.yml");
    }

    @Override
    protected void configure() {
        FileConfiguration config = loadConfigFileFromResource(testConfigName);

        bind(FileConfiguration.class).annotatedWith(Names.named("configFile")).toInstance(config);
    }

    private FileConfiguration loadConfigFileFromResource(String name) {
        return YamlConfiguration.loadConfiguration(new File("src/test/resources/" + name));
    }
}
