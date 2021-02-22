package implementations;

import com.google.inject.AbstractModule;
import com.lauriethefish.betterportals.shared.logging.Logger;
import com.lauriethefish.betterportals.shared.logging.OverrideLogger;

public class TestLoggerModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind all loggers to a simple anonymous logger for testing
        bind(Logger.class).toInstance(new OverrideLogger(Logger.getAnonymousLogger()));
    }
}
