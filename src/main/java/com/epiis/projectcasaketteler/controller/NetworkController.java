package com.epiis.projectcasaketteler.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.epiis.projectcasaketteler.entity.EntityResidence;
import com.epiis.projectcasaketteler.repository.RepositoryResidence;

@RestController
@RequestMapping(path = "casaketteler")
public class NetworkController {

    @Autowired
    private RepositoryResidence repositoryResidence;

    // Este endpoint devuelve la configuración de red esperada
    // El frontend debe comparar con el SSID real del dispositivo
    @GetMapping(path = "network/config")
    public ResponseEntity<Map<String, Object>> getNetworkConfig() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();

        // Estos valores deben configurarse por administrador
        data.put("expectedSSID", "CasaKetteler_WiFi"); // SSID esperado
        data.put("networkType", "WIFI");

        response.put("success", true);
        response.put("data", data);
        response.put("message", "Configuración de red obtenida");

        return ResponseEntity.ok(response);
    }

    // Endpoint para verificar si el SSID proporcionado es válido
    @GetMapping(path = "network/verify")
    public ResponseEntity<Map<String, Object>> verifyNetwork(
            @RequestParam(required = false) String ssid,
            @RequestParam(required = false) String bssid,
            @RequestParam String idResidence) {

        Map<String, Object> response = new HashMap<>();

        Optional<EntityResidence> optional = repositoryResidence.findById(idResidence);

        if (!optional.isPresent()) {
            response.put("success", false);
            response.put("isValid", false);
            response.put("message", "Residencia no encontrada");
            return ResponseEntity.ok(response);
        }

        EntityResidence residence = optional.get();
        boolean isValid = false;

        // Priorizar BSSID si está configurado
        String expectedBSSID = residence.getWifiBssid();
        String expectedSSID = residence.getWifiSsid();

        if (expectedBSSID != null && !expectedBSSID.isEmpty()) {
            isValid = expectedBSSID.equalsIgnoreCase(bssid);
        } else if (expectedSSID != null && !expectedSSID.isEmpty()) {
            isValid = expectedSSID.equals(ssid);
        }

        response.put("success", true);
        response.put("isValid", isValid);
        response.put("message", isValid ? "Red correcta" : "Conéctese a la red oficial de Casa Ketteler");

        return ResponseEntity.ok(response);
    }
}