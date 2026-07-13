/**
 * Lấy ảnh thật (stock photo, có giấy phép sử dụng tự do) từ Pexels API theo
 * từng môn thể thao, dùng làm cover_image_url cho stadium_complexes thay vì
 * ảnh placeholder picsum.photos.
 *
 * Vì sao lấy theo "pool ảnh mỗi môn" thay vì gọi API riêng cho từng sân:
 *  - 161 sân => 161 request là lãng phí quota free tier, dễ bị rate-limit.
 *  - Ảnh thật của từng sân cụ thể không có sẵn hợp pháp để lấy (xem giải
 *    thích trước đó về rủi ro bản quyền khi cào ảnh từ site khác).
 *
 * Pool size (v2 — mở rộng): trước đây cố định 15 ảnh/môn bất kể số sân seed
 * thực tế của môn đó — với Football chiếm 106/161 complex, pool 15 ảnh nghĩa
 * là mỗi ảnh bị dùng lại ~21 lần (106 complex × 3 ảnh / 15 ảnh), rất dễ thấy
 * trùng ảnh khi browse search results. Giờ pool size SCALE theo số complex
 * thực tế của từng môn (xem SPORT_TARGET_COUNTS, khớp counts trong
 * scripts/output/overpass-all.json) + gộp NHIỀU query/môn (không chỉ 1 query
 * hẹp) để tăng đa dạng thay vì chỉ tăng per_page trên cùng 1 query.
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
const DELAY_MS = 1200; // Pexels free tier: 200 req/giờ — nghỉ giữa các query cho chắc
const PEXELS_MAX_PER_PAGE = 80; // giới hạn cứng của Pexels search API

// Số complex thực tế của mỗi môn trong seed hiện tại (161 sân, xem
// scripts/output/overpass-all.json) — dùng để scale pool size hợp lý thay vì
// 1 con số cố định cho mọi môn. Nếu seed thêm complex/môn mới sau này, cập
// nhật lại số này (hoặc tính động từ overpass-all.json nếu muốn tự động hoá).
const SPORT_TARGET_COUNTS = {
  Football: 106,
  Tennis: 23,
  Basketball: 13,
  Badminton: 13,
  Volleyball: 6,
};

// Mỗi ảnh bị dùng lại tối đa ~3 lần là chấp nhận được (đủ đa dạng cho demo) —
// pool size = ceil(số complex thực tế * 3 ảnh mỗi complex / 3 lần lặp lại tối đa).
function poolSizeFor(sportCount) {
  const raw = Math.ceil((sportCount * 3) / 3);
  return Math.min(raw, PEXELS_MAX_PER_PAGE); // Pexels chỉ cho tối đa 80/request
}

// Nhiều query/môn (không chỉ 1 câu hẹp) để tăng đa dạng bố cục/góc chụp thay
// vì lấy per_page lớn trên cùng 1 query hẹp (dễ bị ảnh gần giống nhau).
const SPORT_QUERIES = {
  Football: ["soccer field stadium", "football pitch aerial", "indoor soccer court", "5 a side football"],
  Badminton: ["badminton court", "badminton court indoor"],
  Basketball: ["basketball court", "basketball court outdoor"],
  Tennis: ["tennis court", "tennis court aerial"],
  Volleyball: ["volleyball court", "beach volleyball court"],
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
  const sportEntries = Object.entries(SPORT_QUERIES);

  for (const [sportIdx, [sport, queries]] of sportEntries.entries()) {
    const targetTotal = poolSizeFor(SPORT_TARGET_COUNTS[sport] ?? 15);
    const perQuery = Math.ceil(targetTotal / queries.length);
    console.log(`Đang tìm ảnh cho môn ${sport} (mục tiêu ~${targetTotal} ảnh, ${queries.length} query)...`);

    const seenIds = new Set();
    const photos = [];
    for (const [qIdx, query] of queries.entries()) {
      const batch = await searchPhotos(query, perQuery);
      for (const p of batch) {
        if (!seenIds.has(p.id)) {
          seenIds.add(p.id);
          photos.push(p);
        }
      }
      const isLastQueryOfLastSport = sportIdx === sportEntries.length - 1 && qIdx === queries.length - 1;
      if (!isLastQueryOfLastSport) await sleep(DELAY_MS);
    }

    pool[sport] = photos;
    console.log(`  -> lấy được ${photos.length} ảnh không trùng (mục tiêu ${targetTotal}).`);
  }

  await writeFile(OUTPUT_FILE, JSON.stringify(pool, null, 2), "utf-8");
  console.log(`\nĐã ghi pool ảnh vào ${OUTPUT_FILE}`);
}

main().catch((err) => {
  console.error("Script thất bại:", err.message);
  process.exit(1);
});
