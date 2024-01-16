package com.capitalone.identity.identitybuilder.util;

import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class StringUtils {
    private StringUtils() {
    }

    @NonNull
    public static String requireNotNullOrBlank(String value) {
        if (Strings.isBlank(Objects.requireNonNull(value))) {
            throw new IllegalArgumentException(String.format("empty string provided=[%s]", value));
        } else {
            return value;
        }
    }

    /**
     * Calculate MD5 hash of supplied content
     */
    public static String getContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(content.getBytes());
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
