package com.capitalone.identity.identitybuilder.client.local;

import com.amazonaws.util.IOUtils;
import com.capitalone.identity.identitybuilder.client.DevLocalProperties;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.repository.CommonItemStore;
import com.capitalone.identity.identitybuilder.util.StringUtils;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalDebugItemStore extends CommonItemStore {

    private static Stream<Path> getFileStream(String directory) {
        try {
            return Files.walk(Paths.get(directory));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Optional<String> getLocalConfigDirectory(String rootDir) {
        return Optional.ofNullable(rootDir)
                .filter(dir -> !dir.isEmpty())
                .map(LocalDebugItemStore::getRootDirectory)
                .map(File::getAbsolutePath);
    }

    private static File getRootDirectory(String directory) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(directory);
        if (!resource.exists() && URI.create(directory).getScheme() == null) {
            resource = resourceLoader.getResource("file:" + directory);
        }
        try {
            if (resource.exists() && resource.getFile().exists() && resource.getFile().isDirectory()) {
                return resource.getFile();
            } else {
                final String msg = String.format("Invalid property '%s=%s'. If specified, this directory must exist " +
                                "on the classpath or file system.",
                        DevLocalProperties.DEV_LOCAL_ROOT_DIR, directory);
                throw new IllegalArgumentException(msg);
            }
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    private final String rootDir;

    public LocalDebugItemStore(String rootDir) {
        this.rootDir = getLocalConfigDirectory(rootDir)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Item Store Directory:=" + rootDir));
    }

    @Override
    public Set<ConfigStoreItemInfo> getAllItemInfo() {
        return getConfigStoreItemInfoStream().collect(Collectors.toSet());
    }

    @Override
    public ConfigStoreItem getItem(ConfigStoreItemInfo info) throws IOException {
        Path path = Paths.get(info.getName());
        try (InputStream stream = Files.newInputStream(path)) {
            return new ConfigStoreItem(info, IOUtils.toString(stream));
        }
    }

    @Override
    public Flux<ConfigStoreItemInfo> getStoredItemInfo() {
        return Flux.fromStream(this::getConfigStoreItemInfoStream);
    }

    @Override
    public Optional<ConfigStoreItemInfo> getSingleStoredItemInfo(String key) {
        try {
            return getFileStream(key)
                    .filter(path -> path.toFile().isFile())
                    .findFirst()
                    .map(path -> {
                        String objectLocation = Objects.requireNonNull(path.toString());
                        String tag = getContentHash(path);
                        return new ConfigStoreItemInfo(objectLocation, tag);
                    });
        } catch (RuntimeException e) {
            // Treat this as a catch-all for "item not found" to avoid
            // spamming local development environment with error messages
            // when a file assumed to be there d.n. exist. (see S3ItemStore for
            // more careful treatment of the not found case)
            return Optional.empty();
        }
    }

    private String getContentHash(Path path) {
        try (InputStream stream = Files.newInputStream(path)) {
            String policyContent = IOUtils.toString(stream);
            return StringUtils.getContentHash(policyContent);
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    private Stream<ConfigStoreItemInfo> getConfigStoreItemInfoStream() {
        return getFileStream(rootDir)
                .sorted(Comparator.naturalOrder())
                .filter(path -> path.toFile().isFile())
                .map(path -> {
                    String objectLocation = Objects.requireNonNull(path.toString());
                    String tag = getContentHash(path);
                    return new ConfigStoreItemInfo(objectLocation, tag);
                });
    }

}
