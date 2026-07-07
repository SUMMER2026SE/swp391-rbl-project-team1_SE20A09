package com.sportvenue.util.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VietnamLocationResolverTest {

    private final VietnamLocationResolver resolver = new VietnamLocationResolver();

    @Test
    void resolveProvince_matchesLongerFormThanStoredAddress() {
        // Bug tái hiện: address DB chỉ lưu "Hồ Chí Minh" nhưng user gõ dài hơn — phải vẫn resolve
        // ra cùng 1 canonical để so khớp exact-match hoạt động đúng cho cả 2 phía.
        assertEquals("Hồ Chí Minh", resolver.resolveProvince("Thành phố Hồ Chí Minh"));
        assertEquals("Hồ Chí Minh", resolver.resolveProvince("Hồ Chí Minh"));
        assertEquals("Hồ Chí Minh", resolver.resolveProvince("TP.HCM"));
        assertEquals("Hồ Chí Minh", resolver.resolveProvince("tphcm"));
        assertEquals("Hồ Chí Minh", resolver.resolveProvince("Sài Gòn"));
    }

    @Test
    void resolveProvince_daNangVariants() {
        assertEquals("Đà Nẵng", resolver.resolveProvince("Đà Nẵng"));
        assertEquals("Đà Nẵng", resolver.resolveProvince("thành phố đà nẵng"));
        assertEquals("Đà Nẵng", resolver.resolveProvince("da nang"));
    }

    @Test
    void resolveProvince_unknownCityReturnsNull() {
        assertNull(resolver.resolveProvince("Hà Nội"));
        assertNull(resolver.resolveProvince(""));
        assertNull(resolver.resolveProvince(null));
    }

    @Test
    void resolveDistrict_namedDistrictDiacriticInsensitive() {
        assertEquals("Thủ Đức", resolver.resolveDistrict("Thù Đùc", null));
        assertEquals("Cẩm Lệ", resolver.resolveDistrict("cam le", null));
        assertEquals("Bình Thạnh", resolver.resolveDistrict("binh thanh", null));
    }

    @Test
    void resolveDistrict_numberedDistrictVariants() {
        assertEquals("Quận 1", resolver.resolveDistrict("Quận 1", null));
        assertEquals("Quận 1", resolver.resolveDistrict("q1", null));
        assertEquals("Quận 1", resolver.resolveDistrict("Q.1", null));
        assertEquals("Quận 10", resolver.resolveDistrict("Quận 10", null));
        assertEquals("Quận 12", resolver.resolveDistrict("quan 12", null));
    }

    @Test
    void resolveDistrict_noFalsePositiveBetweenQuan1AndQuan10Or12() {
        // Đây là rủi ro chính đã lường trước: "quan 1" là substring của "quan 10"/"quan 12" —
        // nếu match bằng contains() thô sẽ trả nhầm "Quận 1" khi user thực ra hỏi Quận 10/12.
        assertEquals("Quận 10", resolver.resolveDistrict("Quận 10", null));
        assertEquals("Quận 12", resolver.resolveDistrict("Quận 12", null));
        assertEquals("Quận 1", resolver.resolveDistrict("Quận 1", null));
    }

    @Test
    void deriveFromAddress_realSeedDataExamples() {
        // Ví dụ thật lấy từ V7.6__seed_real_stadiums_osm.sql
        VietnamLocationResolver.LocationMatch match1 =
                resolver.deriveFromAddress("859, Hương Lộ 2, Bình Tân, Hồ Chí Minh");
        assertEquals("Hồ Chí Minh", match1.province());
        assertEquals("Bình Tân", match1.district());

        VietnamLocationResolver.LocationMatch match2 =
                resolver.deriveFromAddress("Trường Sơn, Cẩm Lệ, Đà Nẵng");
        assertEquals("Đà Nẵng", match2.province());
        assertEquals("Cẩm Lệ", match2.district());

        VietnamLocationResolver.LocationMatch match3 =
                resolver.deriveFromAddress("200 Lý Thường Kiệt, Quận 10, TP.HCM");
        assertEquals("Hồ Chí Minh", match3.province());
        assertEquals("Quận 10", match3.district());
    }

    @Test
    void deriveFromAddress_provinceOnlyWhenDistrictMissingFromAddress() {
        // Case thật trong seed data: địa chỉ thiếu tên quận, chỉ có tên thành phố.
        VietnamLocationResolver.LocationMatch match = resolver.deriveFromAddress("Hồ Chí Minh");
        assertEquals("Hồ Chí Minh", match.province());
        assertNull(match.district());
    }

    @Test
    void deriveFromAddress_unresolvableAddressReturnsNulls() {
        VietnamLocationResolver.LocationMatch match = resolver.deriveFromAddress("123 Main Street, Unknown City");
        assertNull(match.province());
        assertNull(match.district());
    }

    @Test
    void supportedLocationDtos_includesNumberedDistrictsForDropdown() {
        var locations = VietnamLocationReference.toSupportedLocationDtos();
        var hcm = locations.stream().filter(l -> l.province().equals("Hồ Chí Minh")).findFirst().orElseThrow();

        assertEquals(2, locations.size());
        assertEquals(true, hcm.districts().contains("Quận 1"));
        assertEquals(true, hcm.districts().contains("Quận 12"));
        assertEquals(true, hcm.districts().contains("Thủ Đức"));
    }
}
