package com.paymentprocessor.fraudservice.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rules")
public class Rule {

    @Id
    private String id;

    private String scope;
    private String name;
    private String expr;
    private String action;
    private Integer priority;
    private Boolean enabled;
    private Integer version;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExpr() { return expr; }
    public void setExpr(String expr) { this.expr = expr; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
