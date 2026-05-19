package com.epiis.projectcasaketteler.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "casaketteler")
public class NetworkController {

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
    public ResponseEntity<Map<String, Object>> verifyNetwork(String ssid) {
        Map<String, Object> response = new HashMap<>();

        String expectedSSID = "CasaKetteler_WiFi"; // Debe venir de BD/configuración

        boolean isValid = expectedSSID.equals(ssid);

        response.put("success", true);
        response.put("isValid", isValid);
        response.put("message", isValid ? "Red correcta" : "Conéctese a la red oficial de Casa Ketteler");

        return ResponseEntity.ok(response);
    }
}