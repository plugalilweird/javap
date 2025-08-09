package com.example.vpnmonitor.controller;

import com.example.vpnmonitor.model.VPNNode;
import com.example.vpnmonitor.repository.VPNNodeRepository;
import com.example.vpnmonitor.service.VPNScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vpn")
public class VPNController {

    @Autowired
    private VPNNodeRepository repository;

    @Autowired
    private VPNScannerService scannerService;

    @GetMapping("/nodes")
    public List<VPNNode> getAllNodes() {
        return repository.findAll();
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, String>> startScan() {
        scannerService.scanAllNodes();
        return ResponseEntity.ok(Map.of("status", "started"));
    }
}