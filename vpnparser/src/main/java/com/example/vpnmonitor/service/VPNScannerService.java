package com.example.vpnmonitor.service;

import com.example.vpnmonitor.model.VPNNode;
import com.example.vpnmonitor.repository.VPNNodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

@Service
public class VPNScannerService {

    @Autowired
    private VPNNodeRepository repository;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String VPNGATE_API = "http://www.vpngate.net/api/iphone/";

    @Scheduled(fixedRateString = "PT30M")
    @Transactional
    public void scanAllNodes() {
        List<VPNNode> newNodes = parseVpnGateNodes();
        if (!newNodes.isEmpty()) {
            repository.saveAll(newNodes);
        }

        List<VPNNode> nodes = repository.findAll();
        for (VPNNode node : nodes) {
            testNode(node);
        }
        repository.saveAll(nodes);
    }

    private List<VPNNode> parseVpnGateNodes() {
        List<VPNNode> result = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(VPNGATE_API, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.err.println("Failed to fetch VPNGate API data: " + response.getStatusCode());
                return result;
            }

            String body = response.getBody();
            try (Scanner scanner = new Scanner(new StringReader(body))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (!line.startsWith("*")) break;
                }

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;

                    String[] cols = line.split(",");
                    if (cols.length < 15) continue;

                    String endpoint = cols[1] + ":" + cols[2];
                    if (repository.findByEndpoint(endpoint).isPresent()) continue;

                    VPNNode node = new VPNNode();
                    node.setName(cols[0]);
                    node.setEndpoint(endpoint);
                    node.setProtocol("OpenVPN");
                    node.setCountry(cols[5]);
                    node.setStatus("unknown");

                    try {
                        node.setBandwidth(Integer.parseInt(cols[6])); // Speed in bps
                    } catch (NumberFormatException e) {
                        node.setBandwidth(0);
                    }
                    try {
                        node.setUptime(Double.parseDouble(cols[7])); // Uptime in milliseconds
                    } catch (NumberFormatException e) {
                        node.setUptime(0.0);
                    }
                    if (cols.length > 14 && !cols[14].isEmpty()) {
                        try {
                            String config = new String(Base64.getDecoder().decode(cols[14]));
                            node.setProtocol(config.contains("udp") ? "OpenVPN-UDP" : "OpenVPN-TCP");
                        } catch (IllegalArgumentException e) {
                            System.err.println("Failed to decode OpenVPN config for " + endpoint);
                        }
                    }

                    result.add(node);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse VPNGate API: " + e.getMessage());
        }
        return result;
    }

    private void testNode(VPNNode node) {
        String[] parts = node.getEndpoint().split(":");
        if (parts.length != 2) {
            node.setStatus("invalid");
            node.setLastCheck(Instant.now());
            return;
        }

        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            node.setStatus("invalid");
            node.setLastCheck(Instant.now());
            return;
        }

        long pingStartTime = System.currentTimeMillis();
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isReachable(3000)) {
                long latency = System.currentTimeMillis() - pingStartTime;
                node.setLatency((int) latency);
                node.setStatus("online");
            } else {
                node.setLatency(null);
                node.setStatus("offline");
            }
        } catch (Exception e) {
            node.setLatency(null);
            node.setStatus("offline");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            node.setStatus("online");
        } catch (Exception e) {
            if (node.getStatus().equals("unknown")) {
                node.setStatus("offline");
            }
        }

        node.setLastCheck(Instant.now());
    }
}