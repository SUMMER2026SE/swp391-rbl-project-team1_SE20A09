package com.sportvenue.controller;

import com.sportvenue.dto.response.LocationDTO;
import com.sportvenue.provider.GeocodeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/geocoding")
@RequiredArgsConstructor
public class GeocodeController {
    
    private final GeocodeProvider geocodeProvider;

    @GetMapping("/search")
    public ResponseEntity<List<LocationDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(geocodeProvider.search(q));
    }

    @GetMapping("/reverse")
    public ResponseEntity<LocationDTO> reverse(@RequestParam Double lat, @RequestParam Double lng) {
        return ResponseEntity.ok(geocodeProvider.reverseGeocode(lat, lng));
    }
}
