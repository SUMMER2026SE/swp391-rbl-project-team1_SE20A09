package com.sportvenue.util.location;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chuẩn hoá tên tỉnh/thành + quận/huyện tự do (tiếng Việt, có/không dấu, viết tắt) về dạng
 * canonical duy nhất trong {@link VietnamLocationReference} — dùng chung cho AI tool, search
 * backend (StadiumSpecification/StadiumComplexSpecification/MatchRequestSpecification), và
 * backfill dữ liệu cũ. Chỉ hỗ trợ 2 thành phố có dữ liệu thật hiện nay.
 */
@Component
public class VietnamLocationResolver {

    /** Kết quả resolve từ 1 chuỗi địa chỉ tự do — field null nếu không xác định được. */
    public record LocationMatch(String province, String district) {
    }

    /**
     * Quận số của TP.HCM (vd "Quận 10", "Q.10", "district 10") — anchor ^...$ để tránh
     * "quan 1" match nhầm vào bên trong "quan 10"/"quan 12".
     */
    private static final Pattern NUMBERED_DISTRICT_PATTERN =
            Pattern.compile("^(q|quan|district|d)\\.?\\s*0?(\\d{1,2})$");

    /** Bỏ dấu tiếng Việt để so khớp không phân biệt dấu (model/user đôi khi đánh dấu không ổn định). */
    public String stripDiacritics(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutMarks = normalized.replaceAll("\\p{M}", "");
        return withoutMarks.replace('đ', 'd').replace('Đ', 'D');
    }

    private String normalize(String text) {
        return stripDiacritics(text).toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /** Resolve tên tỉnh/thành từ 1 chuỗi ngắn (vd tham số district/location model truyền vào). */
    public String resolveProvince(String freeText) {
        if (freeText == null || freeText.isBlank()) {
            return null;
        }
        String key = normalize(freeText);
        for (VietnamLocationReference.Province province : VietnamLocationReference.PROVINCES) {
            if (matchesAlias(key, province.aliases()) || key.equals(normalize(province.canonicalName()))) {
                return province.canonicalName();
            }
        }
        return null;
    }

    /**
     * Resolve tên quận/huyện từ 1 chuỗi ngắn (chỉ chứa tên quận, không phải cả địa chỉ dài).
     * {@code provinceHint} thu hẹp phạm vi tìm khi đã biết tỉnh/thành — truyền {@code null} để
     * tìm trên toàn bộ danh sách.
     */
    public String resolveDistrict(String freeText, String provinceHint) {
        if (freeText == null || freeText.isBlank()) {
            return null;
        }
        String key = normalize(freeText);

        for (VietnamLocationReference.Province province : provincesToSearch(provinceHint)) {
            String numbered = matchNumberedDistrict(key, province);
            if (numbered != null) {
                return numbered;
            }
            for (VietnamLocationReference.District district : province.districts()) {
                if (matchesAlias(key, district.aliases()) || key.equals(normalize(district.canonicalName()))) {
                    return district.canonicalName();
                }
            }
        }
        return null;
    }

    /**
     * Resolve cả province + district từ 1 chuỗi địa chỉ đầy đủ (vd "200 Lý Thường Kiệt, Quận 10,
     * TP.HCM") — dùng cho backfill dữ liệu cũ và khi tạo/sửa complex mới. Địa chỉ được tách theo
     * dấu phẩy để so khớp quận số theo từng token riêng, tránh false-positive.
     */
    public LocationMatch deriveFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return new LocationMatch(null, null);
        }

        String resolvedProvince = resolveProvince(address);
        List<VietnamLocationReference.Province> candidates =
                resolvedProvince != null ? provincesToSearch(resolvedProvince) : VietnamLocationReference.PROVINCES;

        String[] segments = address.split(",");
        String haystack = normalize(address);

        for (VietnamLocationReference.Province candidate : candidates) {
            for (String rawSegment : segments) {
                String numbered = matchNumberedDistrict(normalize(rawSegment), candidate);
                if (numbered != null) {
                    return new LocationMatch(candidate.canonicalName(), numbered);
                }
            }
            for (VietnamLocationReference.District district : candidate.districts()) {
                if (matchesAlias(haystack, district.aliases())) {
                    return new LocationMatch(candidate.canonicalName(), district.canonicalName());
                }
            }
        }
        return new LocationMatch(resolvedProvince, null);
    }

    private List<VietnamLocationReference.Province> provincesToSearch(String provinceHint) {
        if (provinceHint == null) {
            return VietnamLocationReference.PROVINCES;
        }
        String hintKey = normalize(provinceHint);
        return VietnamLocationReference.PROVINCES.stream()
                .filter(p -> normalize(p.canonicalName()).equals(hintKey))
                .findFirst()
                .map(List::of)
                .orElse(VietnamLocationReference.PROVINCES);
    }

    /**
     * Quận 1-12 chỉ tồn tại ở TP.HCM. So khớp bằng anchor ^...$ trên TOÀN BỘ chuỗi truyền vào
     * (không phải contains) vì "quan 1" là substring của "quan 10"/"quan 12".
     */
    private String matchNumberedDistrict(String normalizedSegment, VietnamLocationReference.Province province) {
        if (!VietnamLocationReference.HO_CHI_MINH.canonicalName().equals(province.canonicalName())) {
            return null;
        }
        Matcher matcher = NUMBERED_DISTRICT_PATTERN.matcher(normalizedSegment);
        if (!matcher.matches()) {
            return null;
        }
        int number = Integer.parseInt(matcher.group(2));
        if (number < 1 || number > 12) {
            return null;
        }
        return "Quận " + number;
    }

    /** So khớp alias trong haystack theo TỪ NGUYÊN (word-boundary) — an toàn hơn contains() thô. */
    private boolean matchesAlias(String haystack, List<String> aliases) {
        for (String alias : aliases) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(alias) + "\\b");
            if (pattern.matcher(haystack).find()) {
                return true;
            }
        }
        return false;
    }
}
