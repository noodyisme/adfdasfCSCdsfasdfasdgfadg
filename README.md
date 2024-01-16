# Configuration Store Client · 
[![Build Status](https://identitypl-n1jenkins.cloud.capitalone.com/buildStatus/icon?job=Bogie/identitybuilder/identity-builder-config-store-client/main)](https://identitypl-n1jenkins.cloud.capitalone.com/job/Bogie/job/identitybuilder/job/identity-builder-config-store-client/job/main/)
Config Store client provides an easy to use interface to read configuration files from the Config Store. This client is hosted by artifactory and consumed by client applications.

## Getting Started

These instructions will get your application ready to interface with the config store.

### Installing

##### Maven

```
<dependency>
    <groupId>com.capitalone.identity.identitybuilder</groupId>
    <artifactId>ConfigStoreClient</artifactId>
    <version>{ConfigStoreClientVersion}</version>
</dependency>
```

##### Spring

Add `"com.capitalone.identity.identitybuilder"` to your base package scan.

##### AWS S3 VPC Endpoints

CSC uses VPC endpoints to connect to S3 by default. Refer to this [Pulse page](https://pulse.kdc.capitalone.com/docs/DOC-215400) 
for setup instructions for your instances to connect to the VPC endpoints.

### Environment Variable Setup
>###### Migration to 2.0.3+ version
>
>| From -> To | Notes |
>| --- | --- |
>|  `csc.environment-key -> csc.client-environment` | Now a Required property        |
>|  `csc.bucket-name-override-east-region -> csc.s3.bucket-name.east-region` | Now a Required Property
>|  `csc.bucket-name-override-west-region -> csc.s3.bucket-name.west-region` | Now a Required Property
>|  `csc.local-debug -> csc.dev-local.enabled` | Updated to use `true/false` |
>|  `csc.credential-profile-key -> csc.dev-local.aws-credential-profile-name` |
>|  `csc.local-debug.root-directory -> csc.dev-local.debug-root-directory` |

#### Primary Configuration Properties
| Property                               | Type | Default | Notes |
|----------------------------------------| :---: | :---: | --- |
| `csc.client-environment`               | required | - | options: dev, qa, prod (case insensitive) controls which policies are provided to the client based on Policy Status.
| `csc.s3.bucket-name.east-region`       | required | - | Tell CSC to use this bucket when it detects that it is running in east region
| `csc.s3.bucket-name.west-region`       | required | - | Tells CSC to use this bucket when it detects that it is running in non-east region
| `csc.s3.override-region`               | optional | us-east-1 | Set this property to instruct library to use bucket associated with that region
| `csc.dynamic-updates.time-of-day-utc`  | optional | 02:00:00 | Time of day UTC to target when dynamic updates are applied (ISO-8601, see `java.time.LocalTime.parse(...)`)
| `csc.dynamic-updates.polling-interval` | required | - | Duration between dynamic updates. Must be a factor of 24 hours, example: PT1H, PT6H, PT30M (ISO-8601, see `java.time.Duration.parse(...)`)

#### Feature Flag Configuration Properties
| Property | Type | Default | Notes |
| --- | :---: | :---: | --- |
| `csc.enabled` | optional | true | Flag to disable CSC. No external entities will be loaded.
| `csc.dynamic-updates.enabled` | optional | true | Flag to disable feature that performs runtime updates of Config Store entities.
| `csc.dynamic-updates.external-configuration.enabled` | optional | true | Flag to disable external configuration of `csc.dynamic-updates.time-of-day-utc` and `csc.dynamic-updates.polling-interval`
| `csc.dev-local.enabled` | optional | false | Flag to determine if config store should run in dev-local mode (i.e. use other csc.dev-local properties).
| `csc.aws.proxy.enabled` | optional | false | Flag to initialize the AWS S3 client to use the Capital One proxy. Automatically enabled prior to version 2.5.0

#### Local Development Configuration Properties
       
| Property | Type | Default | Notes |
| --- | :---: | :---: | --- |
| `csc.dev-local.debug-root-directory` | optional | - | If set, instructs CSC to use policies from specified directory and not S3 bucket, e.g. /Users/RVR123/policyX/ 
| `csc.dev-local.aws-credential-profile-name` | optional | - | Required to connect to S3 when dev-local enabled. The profile name for the profile credentials provider. Not required when running on an EC2. e.g., GR_GG_COF_AWS_SharedTech_DigiTech_QA_Developer
>_Local Development Steps_
>1. Set `csc.dev-local.enabled` to enable local development
>2. Configure a source where ConfigStoreClient should look for entities (policies and pips).
> 
>
>    * **Note that you can either have policies fetched from remote AWS S3 bucket or local filesystem, but not both options at the same time**
>
> 
>    * **Option A: retrieve policies from the AWS S3 bucket :**\
        * Set `csc.dev-local.aws-credential-profile-name` in conjunction with bucket names and environment 
>         (see [primary config properties](#primary-configuration-properties)), to use policies and pips 
>         that have been deployed to S3 Buckets.
>    *  **Option B: retrieve policies from the local filesystem:**\
        * Set `csc.local-debug.root-directory` to use a local root directory. 
>       * **Recommended configuration:** a local directory with git repos from 
>           [identitybuilder-policies](https://github.cloud.capitalone.com/identitybuilder-policies) organization. 
>           Create a directory that contains the following git repos:
>           1. [routes](https://github.cloud.capitalone.com/identitybuilder-policies/routes) repo
>           2. Repository of the policy under development
>       * NOTES: Local directory structure needs to obey same namespace conventions of deployed policies, e.g. 
>           policy location and status are defined in metadata.json, process/rules files should be nested under 
>           folders of their item type. Repositories in 
>           [identitybuilder-policies organization](https://github.cloud.capitalone.com/identitybuilder-policies) 
>           typically satisfy these namespacing requirements.
>3. Set `csc.aws.proxy.enabled=true` to connect have the AWS S3 Client connect to S3 via the Capital One proxy

## ConfigStoreClient  Usage
```java
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.ConfigStoreEntityResult;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Disposable;

public class SampleConfigLoader {

    private ConfigStoreClient configStoreClient;

    private Disposable subscription;

    public SampleConfigLoader(@Autowired ConfigStoreClient configStoreClient) {
        this.configStoreClient = configStoreClient;
    }

    /**
     * Loads a snapshot of entities from the ConfigStoreClient and then listens for updates.
     */
    public void load() {
        ConfigStoreEntityResult snapshot = configStoreClient.getEntities();

        snapshot.getStartEntities().forEach(this::addFullEntity);

        subscription = snapshot.getUpdates()
                .subscribe(change -> {
                    switch (change.getType()) {
                        case ADD:
                            addEntity(change.getEntityInfo());
                            return;
                        case UPDATE:
                            updateEntity(change.getEntityInfo());
                            return;
                        case DELETE:
                            removeEntity(change.getEntityInfo());
                            return;
                        default:
                            throw new UnsupportedOperationException("unknown change type");
                    }
                }, error -> System.out.println("There was an error and the update stream terminated."));

    }

    public void unload() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    private void addEntity(EntityInfo info) {
        addFullEntity(configStoreClient.getEntity(info));
    }

    private void updateEntity(EntityInfo info) {
        removeEntity(info);
        addEntity(info);
    }

    private void removeEntity(EntityInfo info) {
        // remove deleted entity
    }

    private void addFullEntity(Entity entity) {
        // use the updated entity
    }
    
}
```

## Interface Documentation

[ConfigStoreClient](src/main/java/com/capitalone/identity/identitybuilder/client/ConfigStoreClient.java)

## Running the tests
ConfigStoreClient uses JUnit with Mockito to test the client functionality.
Mock or local implementations of services are used for all tests that do not end with `IT.java`.

Command line:
```
mvn clean test
```

## Analytics
The following named loggers can be enabled in log4j2.xml settings:

- *identitybuilder.ConfigStoreClient*: Logs errors as well as debug information on received input

*Log4j2.xml Examples*:

    <Logger name = "identitybuilder.ConfigStoreClient" level = "debug">
      <appender-ref ref=["my appender1"]/>
      <appender-ref ref=["my appender2"] />
    </Logger>
## Deployment
* [Library Version and Deployment Docs](https://confluence.kdc.capitalone.com/display/HOF/Client+JAR+Deployment) 

## Design

#### System Level
* [Config Store Design](https://confluence.kdc.capitalone.com/display/HOF/Config+Store) - Config Store Design

#### Config Store Client Library
* [Config Store Client ABAC Design](https://confluence.kdc.capitalone.com/display/Platformers/ABAC+config+store+client+design)
* [Config Store Client V2](https://confluence.kdc.capitalone.com/display/HOF/ConfigStoreClient+Jar+V2.0) - Latest w/ Runtime Entity Updates
* [Config Store Client V1](https://confluence.kdc.capitalone.com/display/HOF/Config+Store+Client+Jar+V1) 
* [Config Store Client Design](https://confluence.kdc.capitalone.com/display/HOF/Client+JAR+Library+Design)
## Built With

* [AWS-JAVA_SDK](https://aws.amazon.com/sdk-for-java/) - AWS Interfacing Client
* [Spring](https://spring.io/) - Used for DI
* [Project Reactor](https://projectreactor.io/) - Reactive Streams in Spring
* [Maven](https://maven.apache.org/) - Dependency Management
* [Apache Commons](http://commons.apache.org/proper/commons-lang/) - Commons
* [Apache Logging](https://logging.apache.org/log4j/2.x/) - Logging

## Test With

* [Mockito](https://site.mockito.org/) - Mocking
* [Java Faker](https://github.com/DiUS/java-faker) - Fake data generator
* [Spring Test](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/testing.html) - Used for Java Reflections

## Authoring Team

Hofund - hofund@capitalone.com
# adfdasfCSCdsfasdfasdgfadg
