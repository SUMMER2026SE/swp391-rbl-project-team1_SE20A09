package com.sportvenue.util;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RelativeDateParser {

    private Clock clock;
    private static final ZoneId TZ = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final Pattern THU_X_TUAN_NAY = Pattern.compile(
            "thứ\\s*(\\d)\\s*tuần\\s*này|tuần\\s*này\\s*thứ\\s*(\\d)|chủ\\s*nhật\\s*tuần\\s*này",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern THU_X_TUAN_SAU = Pattern.compile(
            "thứ\\s*(\\d)\\s*tuần\\s*sau|tuần\\s*sau\\s*thứ\\s*(\\d)|chủ\\s*nhật\\s*tuần\\s*sau",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NGAY_NAY = Pattern.compile(
            "hôm\\s*nay", Pattern.CASE_INSENSITIVE);

    private static final Pattern NGAY_MAI = Pattern.compile(
            "ngày\\s*mai", Pattern.CASE_INSENSITIVE);

    private static final Pattern NGAY_KIA = Pattern.compile(
            "ngày\\s*kia", Pattern.CASE_INSENSITIVE);

    private static final Pattern CUOI_TUAN = Pattern.compile(
            "cuối\\s*tuần|cuối tuần", Pattern.CASE_INSENSITIVE);

    public RelativeDateParser() {
        this.clock = Clock.system(TZ);
    }

    public RelativeDateParser(Clock clock) {
        this.clock = clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDate parse(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String msg = message.toLowerCase().trim();

        LocalDate thisWeek = parseThuTuanNay(msg);
        if (thisWeek != null) {
            return thisWeek;
        }

        LocalDate nextWeek = parseThuTuanSau(msg);
        if (nextWeek != null) {
            return nextWeek;
        }

        // "ngày mai"
        if (NGAY_MAI.matcher(msg).find()) {
            return LocalDate.now(clock).plusDays(1);
        }

        // "ngày kia"
        if (NGAY_KIA.matcher(msg).find()) {
            return LocalDate.now(clock).plusDays(2);
        }

        // "hôm nay"
        if (NGAY_NAY.matcher(msg).find()) {
            return LocalDate.now(clock);
        }

        LocalDate standaloneWeekday = parseStandaloneWeekday(msg);
        if (standaloneWeekday != null) {
            return standaloneWeekday;
        }

        return parseCuoiTuan(msg);
    }

    /** "thứ X tuần này" hoặc "chủ nhật tuần này". */
    private LocalDate parseThuTuanNay(String msg) {
        Matcher m1 = THU_X_TUAN_NAY.matcher(msg);
        if (!m1.find()) {
            return null;
        }
        if (msg.contains("chủ nhật") || msg.contains("cn")) {
            return getNextDateOfDayOfWeek(DayOfWeek.SUNDAY, false);
        }
        int dayOfWeek = extractDayOfWeek(m1);
        if (dayOfWeek > 0) {
            return getNextDateOfDayOfWeek(DayOfWeek.of(dayOfWeek), false);
        }
        return null;
    }

    /** "thứ X tuần sau" hoặc "chủ nhật tuần sau". */
    private LocalDate parseThuTuanSau(String msg) {
        Matcher m2 = THU_X_TUAN_SAU.matcher(msg);
        if (!m2.find()) {
            return null;
        }
        if (msg.contains("chủ nhật") || msg.contains("cn")) {
            return getNextDateOfDayOfWeek(DayOfWeek.SUNDAY, true);
        }
        int dayOfWeek = extractDayOfWeek(m2);
        if (dayOfWeek > 0) {
            return getNextDateOfDayOfWeek(DayOfWeek.of(dayOfWeek), true);
        }
        return null;
    }

    /**
     * "thứ X" standalone — "thứ 2".."thứ 7" là Monday..Saturday (ISO = X-1), "thứ 8" là cách nói
     * colloquial cho Chủ nhật (ISO = 7). Phải trừ 1 giống hệt extractDayOfWeek() ở trên, nếu không
     * "thứ 4" (Wednesday) sẽ bị resolve nhầm thành DayOfWeek.of(4) = Thursday.
     */
    private LocalDate parseStandaloneWeekday(String msg) {
        Matcher m6 = Pattern.compile("thứ\\s*(\\d+)").matcher(msg);
        if (!m6.find()) {
            return null;
        }
        int rawNum = Integer.parseInt(m6.group(1));
        if (rawNum < 2 || rawNum > 8 || msg.contains("tuần sau")) {
            return null;
        }
        int isoDayOfWeek = rawNum == 8 ? 7 : rawNum - 1;
        return getNextDateOfDayOfWeek(DayOfWeek.of(isoDayOfWeek), false);
    }

    /** "cuối tuần". */
    private LocalDate parseCuoiTuan(String msg) {
        if (!CUOI_TUAN.matcher(msg).find()) {
            return null;
        }
        LocalDate todayDow = LocalDate.now(clock);
        DayOfWeek dow = todayDow.getDayOfWeek();

        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return todayDow;
        }
        if (dow == DayOfWeek.FRIDAY) {
            return todayDow.plusDays(1);
        }
        LocalDate result = todayDow.plusDays((DayOfWeek.SATURDAY.getValue() - dow.getValue() + 7) % 7);
        if (result.equals(todayDow)) {
            result = result.plusDays(7);
        }
        return result;
    }

    private int extractDayOfWeek(Matcher m) {
        for (int i = 1; i <= m.groupCount(); i++) {
            String group = m.group(i);
            if (group != null && !group.isBlank()) {
                try {
                    int num = Integer.parseInt(group.trim());
                    if (num == 8) {
                        return 7;
                    }
                    return num - 1;
                } catch (NumberFormatException e) {
                    if ("cn".equalsIgnoreCase(group.trim())) {
                        return 7;
                    }
                }
            }
        }
        return 0;
    }

    private LocalDate getNextDateOfDayOfWeek(DayOfWeek dayOfWeek, boolean nextWeek) {
        LocalDate today = LocalDate.now(clock);
        int todayValue = today.getDayOfWeek().getValue();
        int targetValue = dayOfWeek.getValue();

        int daysUntil;
        if (nextWeek) {
            if (targetValue == 7) {
                daysUntil = (7 - todayValue + 7) % 7;
                if (daysUntil == 0) {
                    daysUntil = 7;
                }
            } else {
                daysUntil = (targetValue - todayValue + 7) % 7;
                if (daysUntil == 0) {
                    daysUntil = 7;
                }
            }
            daysUntil += 7;
        } else {
            daysUntil = (targetValue - todayValue + 7) % 7;
            if (daysUntil == 0) {
                daysUntil = 7;
            }
        }

        return today.plusDays(daysUntil);
    }
}
