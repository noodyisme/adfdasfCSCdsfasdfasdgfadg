package com.capitalone.identity.identitybuilder.polling;

import com.netflix.config.*;
import com.netflix.config.sources.URLConfigurationSource;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class ArchaiusTestSetup implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;
    private static final int POLLING_DELAY_MILLIS = 500;
    private static final int INITIAL_DELAY_MILLIS = 1;
    private static final String s3FileName = "polling" + UUID.randomUUID().toString().substring(0, 6) + ".properties";

    public static ConcurrentCompositeConfiguration getConcurrentCompositeConfiguration() {
        return concurrentCompositeConfiguration;
    }

    private static ConcurrentCompositeConfiguration concurrentCompositeConfiguration;

    public static String getS3FileName() {
        return s3FileName;
    }


    @Override
    public void beforeAll(ExtensionContext context) throws IOException {
        if (!started) {
            started = true;

            File file = new File("src/test/resources/pollingProperties/poll.properties");
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("");
            fileWriter.close();
            URL propertiesFile = file.toURI().toURL();

            PolledConfigurationSource source = new URLConfigurationSource(propertiesFile);
            DynamicConfiguration dynamicConfiguration = new DynamicConfiguration(
                    source, new FixedDelayPollingScheduler(INITIAL_DELAY_MILLIS, POLLING_DELAY_MILLIS, false));

            concurrentCompositeConfiguration = new ConcurrentCompositeConfiguration();
            concurrentCompositeConfiguration.addConfiguration(dynamicConfiguration);

            DynamicPropertyFactory.initWithConfigurationSource(concurrentCompositeConfiguration);

            // The following line registers a callback hook when the root test context is shut down
            context.getRoot().getStore(GLOBAL).put("archaiusTest", this);
        }
    }

    @Override
    public void close() throws IOException {
        File file = new File("src/test/resources/pollingProperties/poll.properties");
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("# placeholder for ArchaiusPollingConfigurationStreamProviderTest");
        fileWriter.close();
    }
}
