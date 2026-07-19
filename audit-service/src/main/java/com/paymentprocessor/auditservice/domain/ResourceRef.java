package com.paymentprocessor.auditservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The resource the action was performed on, as an opaque type + id reference
 * (e.g. {@code payout_account} / {@code pa_...}).
 */
@Embeddable
public class ResourceRef {

    @Column(name = "resource_type")
    private String type;

    @Column(name = "resource_id")
    private String id;

    public ResourceRef() {
    }

    public ResourceRef(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
