package com.capitalone.identity.identitybuilder.model;

/**
 * @deprecated use {@link LogicalVersion}
 */
@Deprecated
public interface PolicyInfo {

    /**
     * Get a short name of a policy. As of 6/9/2021 this value is the parent folder of the metadata.json file where
     * policy versions are specified.
     *
     * @return short name of policy e.g. "customer_overlap"
     */
    String getPolicyShortName();

    /**
     * Get a full name of a policy. A forward-slash-separated namespace that includes tenant, lob, and
     * {@link #getPolicyShortName()}
     *
     * @return full name of policy e.g. "us_consumers/lob_xyz/customer_overlap"
     * @deprecated use {@link LogicalVersion#getName()}
     */
    @Deprecated
    String getPolicyFullName();

    /**
     * Provides policy information to the minor version.
     *
     * @deprecated use {@link LogicalVersion}
     */
    @Deprecated
    interface Patch extends PolicyInfo.Major {

        /**
         * Get version of a policy, at the application-level (as opposed to a concrete resource version
         * like git commit, s3 version last modified, etc.)
         *
         * @return policy version, formatted as positive integers separated by period, e.g. "1.2"
         * @deprecated use {@link LogicalVersion#getMinorVersionString()}
         */
        @Deprecated
        default String getPolicyVersion() {
            return getPolicyMajorVersion() + "." + getPolicyMinorVersion();
        }

        /**
         * @deprecated use {@link LogicalVersion#getMinorVersion()}
         */
        @Deprecated
        int getPolicyMinorVersion();

        /**
         * @deprecated use {@link LogicalVersion#getPatchVersion()}
         */
        @Deprecated
        int getPolicyPatchVersion();

        /**
         * Get version of a policy, at the application-level (as opposed to a concrete resource version
         * like git commit, s3 version last modified, etc.)
         *
         * @return policy version, formatted as positive integers separated by period, e.g. "1.2.3"
         * @deprecated use {@link LogicalVersion#getPatchVersionString()}
         */
        @Deprecated
        default String getPolicyPatchVersionString() {
            return getPolicyMajorVersion() + "." + getPolicyMinorVersion() + "." + getPolicyPatchVersion();
        }

    }

    /**
     * Provides policy information to the minor version.
     *
     * @deprecated use {@link LogicalVersion}
     */
    @Deprecated
    interface Major extends PolicyInfo {
        /**
         * Get version of a policy to the major version level.
         *
         * @return policy major version, a positive integer, e.g. 1, or 2
         * @deprecated use {@link LogicalVersion#getMajorVersion()}
         */
        @Deprecated
        int getPolicyMajorVersion();

    }


}
