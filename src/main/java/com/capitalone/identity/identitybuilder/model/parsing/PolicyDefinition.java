package com.capitalone.identity.identitybuilder.model.parsing;

import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.lang.NonNull;

import java.util.*;


public final class PolicyDefinition implements PolicyInfo.Patch {

    private static final char SEPARATOR_CHAR = '/';
    private static final String SEPARATOR = String.valueOf(SEPARATOR_CHAR);

    /**
     * Short name of policy, e.g. policy_a
     */
    final String shortName;

    /**
     * Full name of policy, e.g. us_consumers/sublob/policy_a
     */
    final String fullName;
    /**
     * Location of policy, e.g. /Users/userA/dev/us_consumers/sublob/policy_a/1.1/32
     */
    final String location;
    final int majorVersion;
    final int minorVersion;
    final int patchVersion;

    public PolicyDefinition(@NonNull String location) {
        this(location.substring(0, location.lastIndexOf('/')),
                location.substring(location.lastIndexOf('/') + 1)
        );
    }

    /**
     * @param policyPath non-null string that represents root of a policy namespace, encompassing all policy versions
     *                   (e.g. us_consumers/card_fraud/customer_overlap) append /<version> to get policy prefix.
     * @throws IllegalArgumentException if prefix ends with a '/' (trailing slash is an implementation detail)
     */
    public PolicyDefinition(@NonNull String policyPath, @NonNull String policyVersion) {

        // location of policy
        policyPath = policyPath.trim();
        if (Strings.isBlank(policyPath) || policyPath.endsWith(SEPARATOR)) {
            String msg = String.format("policy path '%s' cannot be blank and may not end with '%s'", policyPath, SEPARATOR);
            throw new IllegalArgumentException(msg);
        }
        this.location = policyPath + SEPARATOR + policyVersion;

        // populate name and namespace of policy
        String[] pathSplit = policyPath.split(SEPARATOR);

        Deque<String> namespaceStack = new ArrayDeque<>();
        Arrays.asList(pathSplit).forEach(namespaceStack::push);
        String[] fullNameTarget = new String[3];
        for (int i = 2; i >= 0; i--) {
            fullNameTarget[i] = namespaceStack.isEmpty() ? "" : namespaceStack.pop();
        }
        this.fullName = Strings.join(Arrays.asList(fullNameTarget), SEPARATOR_CHAR);
        this.shortName = fullNameTarget[2];

        // version parsing
        final String[] versionFields = policyVersion.split("\\.", 2);
        if (versionFields.length == 2) {
            this.majorVersion = Integer.parseInt(versionFields[0]);
            this.minorVersion = Integer.parseInt(versionFields[1]);
            this.patchVersion = 0;
            if (minorVersion < 0 || majorVersion < 0) {
                throw new IllegalArgumentException(String.format("Invalid major.minor version: '%s'", policyVersion));
            }
        } else {
            throw new IllegalArgumentException(String.format("Invalid major.minor version: '%s'", policyVersion));
        }
    }


    public PolicyDefinition(String location, String policyFullName, String policyShortName, int majorVersion, int minorVersion, int patchVersion) {
        this.location = Objects.requireNonNull(location);
        this.shortName = Objects.requireNonNull(policyShortName);
        this.fullName = Objects.requireNonNull(policyFullName);
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
    }

    @Override
    public String getPolicyShortName() {
        return shortName;
    }

    @Override
    public String getPolicyFullName() {
        return fullName;
    }

    @Override
    public int getPolicyMinorVersion() {
        return minorVersion;
    }

    @Override
    public int getPolicyMajorVersion() {
        return majorVersion;
    }

    /**
     * @return location where this policy can be found, can be file path, s3 prefix, etc.
     */
    public String getPolicyLocation() {
        return location;
    }


    public String getPolicyFullNameWithVersion() {
        return fullName + SEPARATOR + getPolicyVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyDefinition that = (PolicyDefinition) o;
        return location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public int getPolicyPatchVersion() {
        return patchVersion;
    }
}
