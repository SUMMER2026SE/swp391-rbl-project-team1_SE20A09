package com.sportvenue.service.ai;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Đọc nội dung system prompt từ file resource (classpath) thay vì hardcode String nối
 * chuỗi trong Java — pattern "SKILL.md" (ai-patterns/1-prompt-engineering) áp dụng cho
 * Spring: mỗi role có 1 thư mục prompts/&lt;role&gt;/ chứa các file .md nạp thẳng làm
 * system prompt.
 *
 * Nạp 1 lần lúc class-load (gọi từ field initializer {@code static final String X =
 * PromptLoader.load(...)}) — không đọc lại mỗi request, tương đương hardcode String cũ
 * về hiệu năng.
 */
public final class PromptLoader {

    private PromptLoader() {
    }

    /**
     * @param classpathLocation đường dẫn tính từ resources/, ví dụ "prompts/customer/faq.md"
     * @return nội dung file, đã strip() khoảng trắng đầu/cuối — caller tự nối các đoạn
     *         bằng dấu cách/space rõ ràng (không dựa vào khoảng trắng thừa trong file).
     */
    public static String load(String classpathLocation) {
        try {
            ClassPathResource resource = new ClassPathResource(classpathLocation);
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return content.strip();
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được prompt resource: " + classpathLocation, e);
        }
    }
}
