package com.paymentprocessor.disputeservice.entity;

import java.io.Serializable;
import java.util.Objects;

public class ReasonCodeCatalogId implements Serializable {

    private String network;
    private String code;

    public ReasonCodeCatalogId() { }

    public ReasonCodeCatalogId(String network, String code) {
        this.network = network;
        this.code = code;
    }

    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReasonCodeCatalogId that = (ReasonCodeCatalogId) o;
        return Objects.equals(network, that.network) && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() { return Objects.hash(network, code); }
}
