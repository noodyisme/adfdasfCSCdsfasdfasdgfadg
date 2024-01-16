package com.capitalone.identity.identitybuilder.client.local;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.apache.logging.log4j.core.util.Loader.getClassLoader;
import static org.junit.jupiter.api.Assertions.*;

class LocalDebugItemStoreTest {

    String srcDirectory;
    LocalDebugItemStore store;

    @BeforeEach
    void setUpPath() {
        srcDirectory = Objects.requireNonNull(getClassLoader().getResource("test-items")).getPath() + "/";
        store = new LocalDebugItemStore(srcDirectory);
    }

    @Test
    void testConstruct() {
        String rootDir = UUID.randomUUID().toString();
        String msg = assertThrows(IllegalArgumentException.class, () -> new LocalDebugItemStore(rootDir)).getMessage();
        assertTrue(msg.contains(rootDir));
    }

    @Test
    void getAllItemInfo() throws IOException {
        Set<ConfigStoreItemInfo> allItemInfo = store.getAllItemInfo();
        assertNotNull(allItemInfo);

        for (ConfigStoreItemInfo info : allItemInfo) {
            assertNotNull(store.getItem(info));
        }
    }

    @Test
    void getItemInfo_objectExists() {
        String metadataFile = srcDirectory + "metadata.json";
        Optional<ConfigStoreItemInfo> info = store.getSingleStoredItemInfo(metadataFile);
        assertTrue(info.isPresent());
    }

    @Test
    void getItemInfo_objectMissing() {
        Optional<ConfigStoreItemInfo> info = store.getSingleStoredItemInfo("missingFile.txt");
        assertFalse(info.isPresent());
    }

}
