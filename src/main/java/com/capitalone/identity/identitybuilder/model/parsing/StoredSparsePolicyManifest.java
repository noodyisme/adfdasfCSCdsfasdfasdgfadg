package com.capitalone.identity.identitybuilder.model.parsing;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

/**
 * Represent a manifest object that stored inside of policy minor version folder.
 * This file does not represent more than one policy minor version. The status in
 * this file is set directly to {@link EntityActivationStatus}
 */
public class StoredSparsePolicyManifest {

    @JsonProperty(value = "Status")
    @Nullable
    public EntityActivationStatus status;

    @JsonProperty(value = "CompileVersion")
    @Nullable
    public Integer compileVersion;

    @JsonProperty(value = "Type")
    @Nullable
    public String type;

}
