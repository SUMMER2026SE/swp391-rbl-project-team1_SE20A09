/**
 * Lấy ảnh thật (stock photo, có giấy phép sử dụng tự do) từ Pexels API theo
 * từng môn thể thao, dùng làm cover_image_url cho stadium_complexes thay vì
 * ảnh placeholder picsum.photos.
 *
 * Vì sao lấy theo "pool ảnh mỗi môn" thay vì gọi API riêng cho từng sân:
 *  - 161 sân => 161 request là lãng phí quota free tier, dễ bị rate-limit.
 *  - Ảnh thật của từng sân cụ thể không có sẵn hợp pháp để lấy (xem giải
 *    thích trước đó về rủi ro bản quyền khi cào ảnh từ site khác).
 *  - Một pool ~15 ảnh/môn đã đủ đa dạng khi rải ngẫu nhiên cho 161 sân.
 *
 * Chạy (không hardcode key vào file/script):
 *   PEXELS_API_KEY=xxxxx node scripts/fetch-pexels-images.mjs
 *
 * Yêu cầu: Node.js 18+ (dùng fetch built-in).
 */

import { writeFile, mkdir } from "node:fs/promises";
import path from "node:path";

const API_KEY = process.env.PEXELS_API_KEY;
if (!API_KEY) {
  console.error("Thiếu PEXELS_API_KEY. Chạy: PEXELS_API_KEY=xxxxx node scripts/fetch-pexels-images.mjs");
  process.exit(1);
}

const OUTPUT_FILE = path.join(process.cwd(), "scripts", "output", "pexels-images.json");
const IMAGES_PER_SPORT = 15;
const DELAY_MS = 1200; // Pexels free tier: 200 req/giờ — nghỉ giữa các query cho chắc

// Từ khóa tiếng Anh tìm ảnh chuẩn cho từng môn (Pexels index chủ yếu tiếng Anh).
const SPORT_QUERIES = {
  Football: "soccer field stadium",
  Badminton: "badminton court",
  Basketball: "basketball court",
  Tennis: "tennis court",
  Volleyball: "volleyball court",
};

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function searchPhotos(query, perPage) {
  const url = `https://api.pexels.com/v1/search?query=${encodeURIComponent(query)}&per_page=${perPage}&orientation=landscape`;
  const res = await fetch(url, {
    headers: { Authorization: API_KEY },
  });

  if (res.status === 429) {
    console.warn("  Rate limited (429), nghỉ 30s rồi thử lại...");
    await sleep(30000);
    return searchPhotos(query, perPage);
  }

  if (!res.ok) {
    throw new Error(`Pexels API lỗi ${res.status}: ${await res.text()}`);
  }

  const data = await res.json();
  return data.photos.map((p) => ({
    id: p.id,
    url: p.src.large,
    photographer: p.photographer,
    photographerUrl: p.photographer_url,
    pexelsPageUrl: p.url,
    alt: p.alt || query,
  }));
}

async function main() {
  await mkdir(path.dirname(OUTPUT_FILE), { recursive: true });

  const pool = {};
  const sports = Object.entries(SPORT_QUERIES);

  for (const [sport, query] of sports) {
    console.log(`Đang tìm ảnh "${query}" cho môn ${sport}...`);
    const photos = await searchPhotos(query, IMAGES_PER_SPORT);
    pool[sport] = photos;
    console.log(`  -> lấy được ${photos.length} ảnh.`);

    if ([sport, query] !== sports[sports.length - 1]) {
      await sleep(DELAY_MS);
    }
  }

  await writeFile(OUTPUT_FILE, JSON.stringify(pool, null, 2), "utf-8");
  console.log(`\nĐã ghi pool ảnh vào ${OUTPUT_FILE}`);
}

main().catch((err) => {
  console.error("Script thất bại:", err.message);
  process.exit(1);
});
