package com.paymentprocessor.auditservice.domain;

/**
 * Who performed the action. Identifiers are opaque references (e.g. {@code idn_...}) —
 * never names, emails or other raw PII. The IP/user-agent are operational metadata
 * captured for security forensics.
 */
public class Actor {

    private String type;   // e.g. "merchant_user", "system", "service"
    private String id;     // opaque reference, e.g. "idn_..."
    private String ip;
    private String ua;

    public Actor() {
    }

    public Actor(String type, String id, String ip, String ua) {
        this.type = type;
        this.id = id;
        this.ip = ip;
        this.ua = ua;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUa() { return ua; }
    public void setUa(String ua) { this.ua = ua; }
}
