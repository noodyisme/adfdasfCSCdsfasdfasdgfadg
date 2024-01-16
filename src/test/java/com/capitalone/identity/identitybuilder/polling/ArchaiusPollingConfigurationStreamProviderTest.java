package com.capitalone.identity.identitybuilder.polling;

import com.capitalone.identity.identitybuilder.client.s3.S3BucketResolver;
import com.capitalone.identity.identitybuilder.client.s3.S3ConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.ConfigStoreBusinessException;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(ArchaiusTestSetup.class)
@ExtendWith(MockitoExtension.class)
public class ArchaiusPollingConfigurationStreamProviderTest {

    private static final String INITIAL_DURATION = "PT30M";
    private static final String INITIAL_TIME_OF_DAY = "13:00:00";
    private static final String UPDATED_DURATION = "PT2H";
    private static final String UPDATED_TIME_OF_DAY = "08:00:00";
    private static final String gr_gg_cof_aws_sharedTech_digiTech_qa_developer = "GR_GG_COF_AWS_SharedTech_DigiTech_QA_Developer";
    private static final String bucketName = "identitybuilder-core-testbed-configstore-dev-e1";

    @Mock
    S3ConfigurationProperties s3ConfigurationProperties;


    @Test
    void singleUpdate() throws IOException, InterruptedException {
        String durationProperty = "test.duration1";
        String timeOfDayProperty = "test.time-of-day1";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(1);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void updateWithNoInterval() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration2";
        String timeOfDayProperty = "test.time-of-day2";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(1);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, null, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void updateWithNoTimeOfDay() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration3";
        String timeOfDayProperty = "test.time-of-day3";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, null);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, null, currentValue.get());
    }

    @Test
    void noInitialInterval() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration4";
        String timeOfDayProperty = "test.time-of-day4";
        writeProperties(durationProperty, null, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, INITIAL_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void noInitialIntervalPlusValidUpdate() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration5";
        String timeOfDayProperty = "test.time-of-day5";
        writeProperties(durationProperty, null, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void noInitialTimOfDay() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration6";
        String timeOfDayProperty = "test.time-of-day6";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, null);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(INITIAL_DURATION, null, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void emptyFileAtStart() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration7";
        String timeOfDayProperty = "test.time-of-day7";
        writeProperties(durationProperty, null, timeOfDayProperty, null);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, null, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, null);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, null, currentValue.get());

        writeProperties(durationProperty, null, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, UPDATED_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void invalidDuration() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration8";
        String timeOfDayProperty = "test.time-of-day8";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, "Not a duration", timeOfDayProperty, INITIAL_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void invalidTimeOfDay() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration9";
        String timeOfDayProperty = "test.time-of-day9";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, "12:12:12:12");

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);


        TimeUnit.SECONDS.sleep(1);
        checkTimeOfDayAndDuration(INITIAL_DURATION, null, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void emptyFileUpdate() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration10";
        String timeOfDayProperty = "test.time-of-day10";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(1);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, null, timeOfDayProperty, null);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(null, null, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void deleteFile() throws ConfigurationException, IOException, InterruptedException {
        String durationProperty = "test.duration11";
        String timeOfDayProperty = "test.time-of-day11";
        writeProperties(durationProperty, INITIAL_DURATION, timeOfDayProperty, INITIAL_TIME_OF_DAY);

        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(timeOfDayProperty, durationProperty);
        Flux<DynamicUpdateProperties> archaiusPollingConfigurationStreamProviderPollingConfigurationStream = archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream();
        AtomicReference<DynamicUpdateProperties> currentValue = new AtomicReference<>();
        archaiusPollingConfigurationStreamProviderPollingConfigurationStream.subscribeOn(Schedulers.parallel())
                .subscribe(currentValue::set);

        TimeUnit.SECONDS.sleep(1);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        File file = new File("src/test/resources/pollingProperties/poll.properties");
        file.delete();

        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(INITIAL_DURATION, INITIAL_TIME_OF_DAY, currentValue.get());

        writeProperties(durationProperty, UPDATED_DURATION, timeOfDayProperty, UPDATED_TIME_OF_DAY);
        TimeUnit.SECONDS.sleep(2);
        checkTimeOfDayAndDuration(UPDATED_DURATION, UPDATED_TIME_OF_DAY, currentValue.get());
    }

    @Test
    void injectedConstructor() {
        when(s3ConfigurationProperties.getBucketNameForEastRegion()).thenReturn(bucketName);
        when(s3ConfigurationProperties.getCredentialProfileName()).thenReturn(gr_gg_cof_aws_sharedTech_digiTech_qa_developer);
        S3BucketResolver s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        ArchaiusPollingConfigurationStreamProvider archaiusPollingConfigurationStreamProvider = new ArchaiusPollingConfigurationStreamProvider(
            s3BucketResolver, s3ConfigurationProperties, ArchaiusTestSetup.getS3FileName(), false);
        assertEquals(Flux.empty(), archaiusPollingConfigurationStreamProvider.getPollingConfigurationStream());
    }

    @Test
    void injectedConstructorVersionForwarder() {
        when(s3ConfigurationProperties.getBucketNameForEastRegion()).thenReturn("fake_bucket_name");
        when(s3ConfigurationProperties.getCredentialProfileName()).thenReturn(gr_gg_cof_aws_sharedTech_digiTech_qa_developer);
        S3BucketResolver s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        String filename = ArchaiusTestSetup.getS3FileName();
        assertDoesNotThrow(() -> new ArchaiusPollingConfigurationStreamProvider(s3BucketResolver, s3ConfigurationProperties, filename, false));
        assertThrows(ConfigStoreBusinessException.class, () -> new ArchaiusPollingConfigurationStreamProvider(s3BucketResolver, s3ConfigurationProperties, filename, true));
    }

    private void writeProperties(String durationPropertyName, String durationPropertyValue, String timeOfDayPropertyName, String timeOfDayPropertyValue) throws IOException {
        File file = new File("src/test/resources/pollingProperties/poll.properties");
        FileWriter fileWriter = new FileWriter(file);
        if (durationPropertyValue != null && timeOfDayPropertyValue != null) {
            fileWriter.write(durationPropertyName + "=" + durationPropertyValue + "\n"
                    + timeOfDayPropertyName + "=" + timeOfDayPropertyValue);
        } else if (durationPropertyValue != null) {
            fileWriter.write(durationPropertyName + "=" + durationPropertyValue);
        } else if (timeOfDayPropertyValue != null) {
            fileWriter.write(timeOfDayPropertyName + "=" + timeOfDayPropertyValue);
        } else {
            fileWriter.write("");
        }
        fileWriter.close();
    }

    private void checkTimeOfDayAndDuration(String interval, String timeOfDay, DynamicUpdateProperties actual) {

        assertEquals(interval != null ? Duration.parse(interval) : null, actual.getInterval());
        assertEquals(timeOfDay != null ? LocalTime.parse(timeOfDay) : null, actual.getTimeOfDayUTC());
    }
}
