package com.example.vpnmonitor.repository;

import com.example.vpnmonitor.model.VPNNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VPNNodeRepository extends JpaRepository<VPNNode, Long> {
    Optional<VPNNode> findByEndpoint(String endpoint);
}