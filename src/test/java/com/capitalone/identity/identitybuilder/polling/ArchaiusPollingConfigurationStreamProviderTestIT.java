package com.capitalone.identity.identitybuilder.polling;

import com.amazonaws.services.s3.AmazonS3;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.util.AWSTestUtil;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.sources.S3ConfigurationSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Requires token to be fetched for profile.
 */
@ExtendWith(ArchaiusTestSetup.class)
public class ArchaiusPollingConfigurationStreamProviderTestIT {

    static AmazonS3 s3Client;
    private static final String bucketName = "identitybuilder-core-testbed-configstore-dev-e1-gen3";
    private static final String s3FileName = ArchaiusTestSetup.getS3FileName();
    private static final String propertiesFile1 = "pollingFirst.properties";
    private static final String propertiesFile2 = "pollingSecond.properties";

    @BeforeAll
    static void setUp() {
        s3Client = AWSTestUtil.getTestClient();
        s3Client.putObject(bucketName, s3FileName, new File(Objects.requireNonNull(getClassLoader().getResource("test-properties/" + propertiesFile1)).getPath()));

        PolledConfigurationSource source2 = new S3ConfigurationSource(s3Client, bucketName, s3FileName);
        DynamicConfiguration dynamicConfiguration2 = new DynamicConfiguration(
                source2, new FixedDelayPollingScheduler(5000, 5000, false));

        ArchaiusTestSetup.getConcurrentCompositeConfiguration().addConfiguration(dynamicConfiguration2);
    }

    @Test
    void twoUpdates() throws InterruptedException {
        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider();
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration("PT1M", "20:00:00", currentValue.get());

        s3Client.putObject(bucketName, s3FileName, new File(Objects.requireNonNull(getClassLoader().getResource("test-properties/" + propertiesFile2)).getPath()));
        TimeUnit.SECONDS.sleep(11);
        checkTimeOfDayAndDuration("PT60M", "12:00:00", currentValue.get());

        s3Client.putObject(bucketName, s3FileName, new File(Objects.requireNonNull(getClassLoader().getResource("test-properties/" + propertiesFile1)).getPath()));
        TimeUnit.SECONDS.sleep(11);
        checkTimeOfDayAndDuration("PT1M", "20:00:00", currentValue.get());
    }

    private void checkTimeOfDayAndDuration(String interval, String timeOfDay, DynamicUpdateProperties actual) {
        assertEquals(interval != null ? Duration.parse(interval) : null, actual.getInterval());
        assertEquals(timeOfDay != null ? LocalTime.parse(timeOfDay) : null, actual.getTimeOfDayUTC());
    }

    @AfterAll
    static void tearDown(){
        if (s3Client.doesObjectExist(bucketName, s3FileName)) {
            s3Client.deleteObject(bucketName, s3FileName);
        }
    }
}
