package com.capitalone.identity.identitybuilder.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class S3ItemStoreTest {

    @Mock
    AmazonS3 s3Client;

    @Test
    void construct() {
        assertThrows(NullPointerException.class, () -> new S3ItemStore(null, null));
        assertThrows(NullPointerException.class, () -> new S3ItemStore(s3Client, null));
        assertThrows(NullPointerException.class, () -> new S3ItemStore(null, "test"));
    }

}
