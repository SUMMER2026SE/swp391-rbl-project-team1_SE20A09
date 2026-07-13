/**
 * Suy ra địa chỉ đầy đủ hơn (kèm quận/huyện ƯỚC LƯỢNG) cho các complex seed từ
 * OSM chỉ có address = tên thành phố trần trụi (vd "Hà Nội").
 *
 * ⚠️ QUAN TRỌNG — vì sao KHÔNG dùng thẳng quận/huyện trả về từ Nominatim:
 * Việt Nam đã sáp nhập/bãi bỏ cấp hành chính quận/huyện (còn 2 cấp tỉnh/thành
 * → xã/phường) — đã verify trực tiếp bằng cách reverse-geocode toạ độ Nhà thờ
 * Đức Bà (từng là Quận 1, TP.HCM): Nominatim hiện trả về "Phường Sài Gòn,
 * Thành phố Thủ Đức" — "Quận 1" không còn tồn tại trong dữ liệu OSM sống.
 * Dự án này (`VietnamLocationReference.java`) chủ động GIỮ NGUYÊN cấu trúc
 * quận/huyện cũ cho mục đích học thuật/đơn giản hoá — nên script này KHÔNG
 * dùng suburb/city_district từ Nominatim (vì đó là dữ liệu theo cấu trúc mới),
 * mà tự ước lượng quận/huyện CŨ bằng cách so khoảng cách toạ độ thật tới toạ độ
 * trung tâm gần đúng của từng quận/huyện cũ (nearest-centroid, xem
 * DISTRICT_CENTROIDS bên dưới) — toạ độ trung tâm này lấy từ kiến thức địa lý
 * chung, KHÔNG phải dữ liệu ranh giới hành chính đã verify chính xác — chỉ đủ
 * dùng cho seed/demo data, không dùng làm nguồn tham chiếu pháp lý.
 *
 * Vẫn dùng Nominatim để lấy tên đường/số nhà thật (road/house_number) cho phần
 * địa chỉ — thông tin này không đổi theo cải cách ranh giới hành chính nên vẫn
 * đáng tin, chỉ riêng phần quận/huyện là tự ước lượng như trên.
 *
 * KHÔNG cần kết nối DB — match theo (name, latitude, longitude) để update, vì 3
 * giá trị này là unique/ổn định từ lúc seed (V7.6.1), tránh phải cấu hình DB
 * credentials trong script này.
 *
 * Chạy thử trước với vài dòng mẫu (không ghi migration, chỉ in ra + ghi file
 * preview để review):
 *   node scripts/enrich-address-from-coords.mjs --limit=5
 *
 * Chạy đầy đủ (ghi migration V8.1 thật):
 *   node scripts/enrich-address-from-coords.mjs
 *
 * Yêu cầu: Node.js 18+ (dùng fetch built-in, không cần cài thêm package nào).
 * Tôn trọng usage policy của Nominatim (nominatim.org/release-docs/latest/api/Usage-Policy/):
 * tối đa 1 request/giây, có User-Agent định danh rõ ràng, không chạy song song.
 */

import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const INPUT_FILE = path.join(process.cwd(), "scripts", "output", "overpass-all.json");
const OUTPUT_DIR = path.join(process.cwd(), "scripts", "output");
const MIGRATION_FILE = path.join(
  process.cwd(),
  "backend", "src", "main", "resources", "db", "migration",
  "V8.1__enrich_complex_addresses_from_coords.sql"
);

const NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";
const REQUEST_DELAY_MS = 1100; // Nominatim usage policy: tối đa 1 req/giây
const USER_AGENT = "sportvenue-seed-script/1.0 (student project, address enrichment; contact: see repo)";

// Chỉ những sportType đã thực sự được seed vào DB (khớp filter trong
// generate-seed-sql-from-osm.mjs:177) mới cần enrich — venue khác trong file
// cache không tồn tại trong DB.
const SEEDED_SPORTS = new Set(["Football", "Badminton", "Basketball", "Tennis", "Volleyball"]);
const BARE_CITY_ADDRESSES = new Set(["Hà Nội", "Đà Nẵng", "Hồ Chí Minh"]);

// Toạ độ trung tâm ƯỚC LƯỢNG (không phải ranh giới hành chính đã verify) của
// từng quận/huyện CŨ — dùng để tìm quận/huyện gần nhất theo toạ độ thật của
// complex (nearest-centroid). Danh sách quận/huyện khớp với
// VietnamLocationReference.java (backend/.../util/location/) để giá trị ghi
// vào DB tương thích với dropdown filter/search hiện có.
const DISTRICT_CENTROIDS = {
  "Hà Nội": {
    "Ba Đình": [21.0335, 105.8140], "Hoàn Kiếm": [21.0285, 105.8542],
    "Tây Hồ": [21.0687, 105.8228], "Long Biên": [21.0450, 105.8900],
    "Cầu Giấy": [21.0308, 105.7981], "Đống Đa": [21.0158, 105.8280],
    "Hai Bà Trưng": [21.0075, 105.8570], "Hoàng Mai": [20.9764, 105.8500],
    "Thanh Xuân": [20.9950, 105.8060], "Nam Từ Liêm": [21.0100, 105.7650],
    "Bắc Từ Liêm": [21.0620, 105.7550], "Hà Đông": [20.9540, 105.7710],
    "Sơn Tây": [21.1370, 105.5030], "Sóc Sơn": [21.2670, 105.8410],
    "Đông Anh": [21.1360, 105.8480], "Gia Lâm": [21.0270, 105.9440],
    "Thanh Trì": [20.9330, 105.8460], "Mê Linh": [21.1830, 105.7180],
    "Thường Tín": [20.8760, 105.8660], "Phú Xuyên": [20.7420, 105.9110],
    "Ứng Hòa": [20.7080, 105.7580], "Mỹ Đức": [20.6900, 105.7280],
    "Thanh Oai": [20.8630, 105.7700], "Chương Mỹ": [20.8990, 105.6790],
    "Quốc Oai": [21.0290, 105.6250], "Thạch Thất": [21.0910, 105.5760],
    "Phúc Thọ": [21.1150, 105.5490], "Đan Phượng": [21.0980, 105.6660],
    "Hoài Đức": [21.0330, 105.6910], "Ba Vì": [21.2140, 105.4290],
  },
  "Đà Nẵng": {
    "Hải Châu": [16.0678, 108.2208], "Thanh Khê": [16.0630, 108.1930],
    "Sơn Trà": [16.1060, 108.2620], "Ngũ Hành Sơn": [16.0020, 108.2560],
    "Liên Chiểu": [16.0730, 108.1420], "Cẩm Lệ": [16.0130, 108.2020],
    "Hòa Vang": [15.9700, 108.0500],
  },
  "Hồ Chí Minh": {
    "Quận 1": [10.7769, 106.7009], "Quận 2": [10.7870, 106.7497],
    "Quận 3": [10.7843, 106.6866], "Quận 4": [10.7580, 106.7040],
    "Quận 5": [10.7550, 106.6640], "Quận 6": [10.7460, 106.6350],
    "Quận 7": [10.7340, 106.7220], "Quận 8": [10.7230, 106.6280],
    "Quận 9": [10.8410, 106.8090], "Quận 10": [10.7720, 106.6670],
    "Quận 11": [10.7630, 106.6500], "Quận 12": [10.8670, 106.6410],
    "Thủ Đức": [10.8500, 106.7550], "Bình Thạnh": [10.8100, 106.7090],
    "Phú Nhuận": [10.7990, 106.6800], "Gò Vấp": [10.8390, 106.6650],
    "Tân Bình": [10.8010, 106.6520], "Tân Phú": [10.7900, 106.6280],
    "Bình Tân": [10.7650, 106.6030], "Bình Chánh": [10.6890, 106.5940],
    "Hóc Môn": [10.8850, 106.5950], "Củ Chi": [10.9730, 106.5100],
    "Nhà Bè": [10.6960, 106.7420], "Cần Giờ": [10.4110, 106.9560],
  },
};

/** Haversine đơn giản — chỉ cần so sánh khoảng cách tương đối, không cần độ chính xác m/km tuyệt đối. */
function distanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/** Ước lượng quận/huyện cũ gần nhất theo toạ độ thật — KHÔNG phải tra ranh giới chính xác. */
function estimateNearestDistrict(lat, lon, city) {
  const centroids = DISTRICT_CENTROIDS[city];
  if (!centroids) return { district: null, distanceKm: null };
  let best = null;
  let bestDist = Infinity;
  for (const [district, [cLat, cLon]] of Object.entries(centroids)) {
    const d = distanceKm(lat, lon, cLat, cLon);
    if (d < bestDist) {
      bestDist = d;
      best = district;
    }
  }
  return { district: best, distanceKm: Math.round(bestDist * 10) / 10 };
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function reverseGeocode(lat, lon, attempt = 1) {
  const url = `${NOMINATIM_URL}?format=jsonv2&lat=${lat}&lon=${lon}&addressdetails=1&accept-language=vi&zoom=16`;
  const res = await fetch(url, { headers: { "User-Agent": USER_AGENT } });

  if (res.status === 429 && attempt <= 3) {
    const backoff = REQUEST_DELAY_MS * attempt * 2;
    console.warn(`  Nominatim 429, thử lại sau ${backoff}ms (lần ${attempt}/3)...`);
    await sleep(backoff);
    return reverseGeocode(lat, lon, attempt + 1);
  }
  if (!res.ok) {
    throw new Error(`Nominatim lỗi ${res.status}: ${await res.text()}`);
  }
  return res.json();
}

function buildEnrichedAddress(nominatimAddress, city, resolvedDistrict) {
  const parts = [
    nominatimAddress.house_number,
    nominatimAddress.road,
    resolvedDistrict,
    city,
  ].filter(Boolean);
  return parts.join(", ");
}

function sqlEscape(value) {
  return String(value).replace(/'/g, "''");
}

async function main() {
  const limitArg = process.argv.find((a) => a.startsWith("--limit="));
  const limit = limitArg ? Number(limitArg.split("=")[1]) : null;
  const isDryRun = limit !== null;

  const raw = await readFile(INPUT_FILE, "utf-8");
  const allVenues = JSON.parse(raw);

  const seeded = allVenues.filter((v) => v.sportTypeMapped && SEEDED_SPORTS.has(v.sportTypeMapped));
  const candidates = seeded.filter((v) => BARE_CITY_ADDRESSES.has(v.address));

  console.log(`Tổng venue đã seed: ${seeded.length}. Cần enrich (address = tên thành phố trần trụi): ${candidates.length}.`);

  const toProcess = isDryRun ? candidates.slice(0, limit) : candidates;
  if (isDryRun) {
    console.log(`--- DRY RUN: chỉ xử lý ${toProcess.length} venue đầu tiên, không ghi migration ---\n`);
  }

  const results = [];
  for (const [i, venue] of toProcess.entries()) {
    console.log(`[${i + 1}/${toProcess.length}] ${venue.name} (${venue.city}) @ ${venue.latitude},${venue.longitude}`);
    try {
      const geo = await reverseGeocode(venue.latitude, venue.longitude);
      const address = geo.address ?? {};
      const { district: estimatedDistrict, distanceKm: centroidDistanceKm } = estimateNearestDistrict(
        venue.latitude,
        venue.longitude,
        venue.city
      );
      const enrichedAddress = buildEnrichedAddress(address, venue.city, estimatedDistrict);

      results.push({
        osmId: venue.osmId,
        name: venue.name,
        latitude: venue.latitude,
        longitude: venue.longitude,
        city: venue.city,
        oldAddress: venue.address,
        nominatimDisplayName: geo.display_name ?? null,
        estimatedDistrict,
        estimatedDistrictDistanceKm: centroidDistanceKm,
        newAddress: enrichedAddress,
        // >15km tới centroid gần nhất là dấu hiệu ước lượng có thể kém tin cậy
        // (toạ độ nằm ở rìa/giữa 2 quận) — vẫn ghi nhưng đánh dấu để review thủ công.
        status: centroidDistanceKm !== null && centroidDistanceKm <= 15 ? "RESOLVED" : "LOW_CONFIDENCE",
      });

      console.log(`  -> quận ước lượng: ${estimatedDistrict ?? "(không xác định được)"} (~${centroidDistanceKm}km tới centroid) | address mới: ${enrichedAddress}`);
    } catch (err) {
      console.error(`  Lỗi: ${err.message}`);
      results.push({
        osmId: venue.osmId,
        name: venue.name,
        latitude: venue.latitude,
        longitude: venue.longitude,
        city: venue.city,
        oldAddress: venue.address,
        status: "ERROR",
        error: err.message,
      });
    }
    if (i < toProcess.length - 1) await sleep(REQUEST_DELAY_MS);
  }

  const resolvedCount = results.filter((r) => r.status === "RESOLVED").length;
  const lowConfidenceCount = results.filter((r) => r.status === "LOW_CONFIDENCE").length;
  const errorCount = results.filter((r) => r.status === "ERROR").length;
  console.log(`\nKết quả: ${resolvedCount} resolved, ${lowConfidenceCount} low-confidence (>15km tới centroid, cần review thủ công), ${errorCount} lỗi.`);

  const previewFile = path.join(OUTPUT_DIR, isDryRun ? "address-enrichment-preview.json" : "address-enrichment.json");
  await writeFile(previewFile, JSON.stringify(results, null, 2), "utf-8");
  console.log(`Đã ghi ${previewFile}`);

  if (isDryRun) {
    console.log("\nDry run xong — review file preview ở trên rồi chạy lại KHÔNG kèm --limit để xử lý toàn bộ + sinh migration thật.");
    return;
  }

  const updatable = results.filter((r) => r.status === "RESOLVED" || r.status === "LOW_CONFIDENCE");
  const sqlLines = [
    "-- V8.1__enrich_complex_addresses_from_coords.sql",
    "-- Sinh tự động bởi scripts/enrich-address-from-coords.mjs cho các complex seed từ",
    "-- V7.6.1 chỉ có address = tên thành phố trần trụi (OSM gốc không có tag addr:*",
    "-- chi tiết cho các địa điểm này).",
    "--",
    "-- address: ghép từ road/house_number THẬT lấy qua Nominatim reverse-geocode.",
    "-- district: ƯỚC LƯỢNG bằng nearest-centroid (toạ độ thật của complex so với toạ độ",
    "-- trung tâm gần đúng của từng quận/huyện CŨ) — KHÔNG phải tra cứu ranh giới hành",
    "-- chính chính xác. Lý do phải ước lượng thay vì lấy trực tiếp từ Nominatim: Việt Nam",
    "-- đã sáp nhập/bãi bỏ cấp quận/huyện ngoài thực tế (chỉ còn tỉnh/thành -> xã/phường),",
    "-- nên Nominatim hiện trả về cấu trúc MỚI không khớp với VietnamLocationReference.java",
    "-- (dự án chủ động giữ cấu trúc quận/huyện CŨ cho mục đích học thuật/đơn giản hoá).",
    `-- ${errorCount} complex bị lỗi request, giữ nguyên address cũ — xem`,
    "-- scripts/output/address-enrichment.json (status=ERROR) nếu cần xử lý lại.",
    "-- Match theo (name, latitude, longitude) — 3 giá trị ổn định từ lúc seed, không cần biết complex_id.",
    "",
  ];
  for (const r of updatable) {
    sqlLines.push(
      `UPDATE stadium_complexes SET address = '${sqlEscape(r.newAddress)}', district = '${sqlEscape(r.estimatedDistrict)}' ` +
      `WHERE name = '${sqlEscape(r.name)}' AND latitude = ${r.latitude} AND longitude = ${r.longitude} AND district IS NULL;`
    );
  }
  await writeFile(MIGRATION_FILE, sqlLines.join("\n") + "\n", "utf-8");
  console.log(`Đã ghi migration: ${MIGRATION_FILE} (${updatable.length} UPDATE statements, trong đó ${lowConfidenceCount} low-confidence nên review thêm).`);
  console.log(`Còn ${errorCount} venue lỗi request — xem chi tiết trong ${previewFile}.`);
}

main().catch((err) => {
  console.error("Script thất bại:", err.message);
  process.exit(1);
});
