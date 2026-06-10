package com.sportvenue.util;

import com.sportvenue.entity.Stadium;
import com.sportvenue.entity.StadiumImage;
import java.util.Comparator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StadiumUtils {

    public static String resolveImageUrl(Stadium stadium) {
        if (stadium.getImages() == null || stadium.getImages().isEmpty()) {
            return "https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800";
        }
        return stadium.getImages().stream()
                .sorted(Comparator.comparing(StadiumImage::getUploadedAt))
                .map(StadiumImage::getImageUrl)
                .findFirst()
                .orElse("https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=800");
    }

    public static String toSportLabel(String sportName) {
        return switch (sportName) {
            case "Football" -> "Bóng đá";
            case "Badminton" -> "Cầu lông";
            case "Basketball" -> "Bóng rổ";
            case "Tennis" -> "Quần vợt";
            case "Pickleball" -> "Pickleball";
            default -> sportName;
        };
    }
}
