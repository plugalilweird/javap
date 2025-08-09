package com.example.vpnmonitor.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vpn_nodes")
public class VPNNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String endpoint;
    private String protocol;
    private String country;
    private Integer latency;
    private Integer bandwidth;
    private Double uptime;
    private String status;
    private Instant lastCheck;

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    public VPNNode() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getLatency() {
        return latency;
    }

    public void setLatency(Integer latency) {
        this.latency = latency;
    }

    public Integer getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
    }

    public Double getUptime() {
        return uptime;
    }

    public void setUptime(Double uptime) {
        this.uptime = uptime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(Instant lastCheck) {
        this.lastCheck = lastCheck;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}