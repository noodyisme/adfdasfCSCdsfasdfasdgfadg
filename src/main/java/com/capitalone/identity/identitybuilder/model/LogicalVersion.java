package com.capitalone.identity.identitybuilder.model;

public interface LogicalVersion {

    /**
     * @return name of the entity that does not include version information
     */
    String getName();

    int getMajorVersion();

    int getMinorVersion();

    int getPatchVersion();

    default String getLogicalVersionString(String separator) {
        return getName() + separator + getPatchVersionString();
    }

    default String getMajorVersionString() {
        return Integer.toString(getMajorVersion());
    }

    default String getMinorVersionString() {
        return getMajorVersion() + "." + getMinorVersion();
    }

    default String getPatchVersionString() {
        return getMajorVersion() + "." + getMinorVersion() + "." + getPatchVersion();
    }

}
