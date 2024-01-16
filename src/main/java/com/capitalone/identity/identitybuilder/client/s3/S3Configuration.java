package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.capitalone.identity.identitybuilder.client.*;
import com.capitalone.identity.identitybuilder.polling.ScanRequester;
import com.capitalone.identity.identitybuilder.repository.EntityProvider;
import com.capitalone.identity.identitybuilder.repository.ItemStore;
import com.capitalone.identity.identitybuilder.util.AWSUtil;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Configuration
@Conditional(S3Configuration.S3Enabled.class)
@Import(ConfigStoreClientConfiguration.class)
@ComponentScan
public class S3Configuration {

    @Bean
    @Primary
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    ConfigStoreClient getS3Client(S3ConfigurationProperties s3ConfigurationProperties,
                                  Optional<ScanRequester> scanRequester,
                                  Optional<ConfigStoreClient_ApplicationEventPublisher> publisher) {

        S3BucketResolver s3BucketResolver = new S3BucketResolver(s3ConfigurationProperties);
        AmazonS3 s3Client = AWSUtil.createAmazonS3Client(s3BucketResolver.getRegions(),
                s3ConfigurationProperties.getCredentialProfileName(),
                s3ConfigurationProperties.getIsProxyEnabled());
        ItemStore s3Store = new S3ItemStore(s3Client, s3BucketResolver.getBucketName());
        EntityProvider entityProvider = new EntityProvider(s3Store,
                scanRequester.orElse(Flux::never),
                publisher.orElse(ConfigStoreClient_ApplicationEventPublisher.EMPTY),
                s3ConfigurationProperties.getClientEnvironment()
        );

        return new ConfigStoreClientImpl(entityProvider);
    }

    public static class S3Enabled extends AllNestedConditions {

        S3Enabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @Conditional(ClientProperties.LibraryEnabled.class)
        public static class CscEnabled {
        }

        @Conditional(DevLocalProperties.DevLocalDebugRootDirectoryDisabled.class)
        public static class NoLocalStorePresent {
        }
    }
}
