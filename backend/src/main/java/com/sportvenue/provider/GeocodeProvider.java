package com.sportvenue.provider;

import com.sportvenue.dto.response.LocationDTO;
import java.util.List;

public interface GeocodeProvider {
    List<LocationDTO> search(String query);

    LocationDTO reverseGeocode(Double lat, Double lng);
}
