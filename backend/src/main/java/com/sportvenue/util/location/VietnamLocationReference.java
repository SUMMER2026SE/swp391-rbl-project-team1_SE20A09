package com.sportvenue.util.location;

import com.sportvenue.dto.response.SupportedLocationDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu tham chiếu tĩnh cho 2 thành phố hiện có dữ liệu sân thật: TP. Hồ Chí Minh và Đà Nẵng.
 * Alias được lưu ở dạng đã bỏ dấu + viết thường để so khớp trực tiếp với chuỗi đã qua
 * {@link VietnamLocationResolver#stripDiacritics(String)}. Mở rộng thêm thành phố mới chỉ cần
 * thêm 1 {@link Province} vào {@link #PROVINCES}.
 *
 * Quận 1-12 của TP.HCM có mặt trong danh sách này (phục vụ dropdown ở /public/locations), nhưng
 * việc SO KHỚP thực tế cho các quận số này luôn đi qua regex anchor trong
 * {@link VietnamLocationResolver#resolveDistrict}/{@code deriveFromAddress} TRƯỚC — vì "quan 1"
 * là substring của "quan 10"/"quan 12", alias contains() thô (dù đã có \b word-boundary) vẫn kém
 * tin cậy hơn regex anchor ^...$ dùng riêng cho input dạng viết tắt (Q.1, Q1, quan 01...).
 */
public final class VietnamLocationReference {

    public record District(String canonicalName, List<String> aliases) {}

    public record Province(String canonicalName, List<String> aliases, List<District> districts) {}

    private static List<District> numberedDistricts(int fromInclusive, int toInclusive) {
        List<District> districts = new ArrayList<>();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            districts.add(new District("Quận " + i, List.of("quan " + i, "q" + i)));
        }
        return districts;
    }

    public static final Province HO_CHI_MINH = new Province(
            "Hồ Chí Minh",
            List.of("ho chi minh", "tp hcm", "tp.hcm", "tphcm", "thanh pho ho chi minh", "sai gon", "saigon"),
            concat(
                    numberedDistricts(1, 12),
                    List.of(
                            new District("Thủ Đức", List.of("thu duc")),
                            new District("Bình Thạnh", List.of("binh thanh")),
                            new District("Phú Nhuận", List.of("phu nhuan")),
                            new District("Gò Vấp", List.of("go vap")),
                            new District("Tân Bình", List.of("tan binh")),
                            new District("Tân Phú", List.of("tan phu")),
                            new District("Bình Tân", List.of("binh tan")),
                            new District("Bình Chánh", List.of("binh chanh")),
                            new District("Hóc Môn", List.of("hoc mon")),
                            new District("Củ Chi", List.of("cu chi")),
                            new District("Nhà Bè", List.of("nha be")),
                            new District("Cần Giờ", List.of("can gio"))
                    )
            )
    );

    private static List<District> concat(List<District> a, List<District> b) {
        List<District> result = new ArrayList<>(a);
        result.addAll(b);
        return List.copyOf(result);
    }

    public static final Province DA_NANG = new Province(
            "Đà Nẵng",
            List.of("da nang", "tp da nang", "tp.da nang", "thanh pho da nang"),
            List.of(
                    new District("Hải Châu", List.of("hai chau")),
                    new District("Thanh Khê", List.of("thanh khe")),
                    new District("Sơn Trà", List.of("son tra")),
                    new District("Ngũ Hành Sơn", List.of("ngu hanh son")),
                    new District("Liên Chiểu", List.of("lien chieu")),
                    new District("Cẩm Lệ", List.of("cam le")),
                    new District("Hòa Vang", List.of("hoa vang"))
            )
    );

    public static final List<Province> PROVINCES = List.of(HO_CHI_MINH, DA_NANG);

    /** Dùng bởi PublicLocationController để trả danh sách tỉnh/thành cho dropdown ở frontend. */
    public static List<SupportedLocationDto> toSupportedLocationDtos() {
        return PROVINCES.stream()
                .map(p -> new SupportedLocationDto(
                        p.canonicalName(),
                        p.districts().stream().map(District::canonicalName).toList()))
                .toList();
    }

    private VietnamLocationReference() {
    }
}
