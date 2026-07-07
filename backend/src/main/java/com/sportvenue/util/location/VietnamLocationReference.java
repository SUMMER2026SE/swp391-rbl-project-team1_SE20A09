package com.sportvenue.util.location;

import com.sportvenue.dto.response.SupportedLocationDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu tham chiếu tĩnh cho các thành phố được hỗ trợ tìm kiếm: TP. Hồ Chí Minh, Đà Nẵng và Hà Nội.
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

    public static final Province HA_NOI = new Province(
            "Hà Nội",
            List.of("ha noi", "tp ha noi", "tp.ha noi", "thanh pho ha noi", "thu do", "hn"),
            List.of(
                    // 12 quận nội thành
                    new District("Ba Đình",       List.of("ba dinh")),
                    new District("Hoàn Kiếm",     List.of("hoan kiem")),
                    new District("Tây Hồ",        List.of("tay ho")),
                    new District("Long Biên",      List.of("long bien")),
                    new District("Cầu Giấy",      List.of("cau giay")),
                    new District("Đống Đa",        List.of("dong da")),
                    new District("Hai Bà Trưng",   List.of("hai ba trung")),
                    new District("Hoàng Mai",      List.of("hoang mai")),
                    new District("Thanh Xuân",     List.of("thanh xuan")),
                    new District("Nam Từ Liêm",    List.of("nam tu liem")),
                    new District("Bắc Từ Liêm",    List.of("bac tu liem")),
                    new District("Hà Đông",        List.of("ha dong")),
                    // Thị xã
                    new District("Sơn Tây",        List.of("son tay")),
                    // 17 huyện ngoại thành
                    new District("Sóc Sơn",        List.of("soc son")),
                    new District("Đông Anh",       List.of("dong anh")),
                    new District("Gia Lâm",        List.of("gia lam")),
                    new District("Thanh Trì",      List.of("thanh tri")),
                    new District("Mê Linh",        List.of("me linh")),
                    new District("Thường Tín",     List.of("thuong tin")),
                    new District("Phú Xuyên",      List.of("phu xuyen")),
                    new District("Ứng Hòa",        List.of("ung hoa")),
                    new District("Mỹ Đức",         List.of("my duc")),
                    new District("Thanh Oai",      List.of("thanh oai")),
                    new District("Chương Mỹ",      List.of("chuong my")),
                    new District("Quốc Oai",       List.of("quoc oai")),
                    new District("Thạch Thất",     List.of("thach that")),
                    new District("Phúc Thọ",       List.of("phuc tho")),
                    new District("Đan Phượng",     List.of("dan phuong")),
                    new District("Hoài Đức",       List.of("hoai duc")),
                    new District("Ba Vì",          List.of("ba vi"))
            )
    );

    public static final List<Province> PROVINCES = List.of(HO_CHI_MINH, DA_NANG, HA_NOI);

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
