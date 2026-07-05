/**
 * Cào dữ liệu sân thể thao thật ở Hà Nội, Đà Nẵng, TP.HCM từ OpenStreetMap
 * (qua Overpass API) — dùng làm seed data cho bảng stadium_complexes / stadiums.
 *
 * Chạy: node scripts/fetch-overpass-stadiums.mjs
 * Yêu cầu: Node.js 18+ (dùng fetch built-in, không cần cài thêm package nào).
 *
 * Overpass API là dịch vụ công cộng miễn phí — script này cố tình:
 *  - Chỉ chạy tuần tự (không song song) giữa các thành phố
 *  - Nghỉ giữa các request (OVERPASS_DELAY_MS) để không làm quá tải server
 *  - Retry có backoff khi bị 429 (rate limit) hoặc 504 (server bận)
 * Không tự ý giảm delay hoặc chạy song song nhiều instance — sẽ bị chặn IP.
 */

import { writeFile, mkdir } from "node:fs/promises";
import path from "node:path";

// Lưu ý: overpass-api.de (server chính) hay chặn IP của cloud/datacenter (trả về 406).
// Nếu gặp lỗi 406 khi chạy, đổi sang 1 trong các mirror công cộng khác:
//   https://z.overpass-api.de/api/interpreter   (đã test hoạt động ổn)
//   https://lz4.overpass-api.de/api/interpreter
//   https://overpass.kumi.systems/api/interpreter
const OVERPASS_URL = "https://z.overpass-api.de/api/interpreter";
const OUTPUT_DIR = path.join(process.cwd(), "scripts", "output");
const OVERPASS_DELAY_MS = 5000; // nghỉ 5s giữa mỗi city để lịch sự với server free
const MAX_RETRIES = 3;

// Bounding box = [south, west, north, east]. Đây là vùng lõi đô thị của mỗi
// thành phố — có thể mở rộng nếu muốn lấy cả vùng ven.
const CITIES = [
  { name: "Hà Nội", slug: "ha-noi", bbox: [20.95, 105.75, 21.10, 105.90] },
  { name: "Đà Nẵng", slug: "da-nang", bbox: [15.95, 108.10, 16.15, 108.30] },
  { name: "Hồ Chí Minh", slug: "ho-chi-minh", bbox: [10.70, 106.55, 10.90, 106.80] },
];

// Map giá trị tag "sport" của OSM sang sport_name đang có trong bảng sport_types
// (Football, Badminton, Basketball, Tennis, Volleyball — xem V2__seed_data.sql).
// Giá trị nào không map được sẽ giữ nguyên rawSport để bạn tự phân loại thủ công.
const SPORT_TAG_MAP = {
  soccer: "Football",
  football: "Football",
  badminton: "Badminton",
  basketball: "Basketball",
  tennis: "Tennis",
  volleyball: "Volleyball",
};

function buildQuery([south, west, north, east]) {
  const bbox = `${south},${west},${north},${east}`;
  // Chỉ lấy các node/way có "name" — tránh sân vô danh không dùng làm seed được.
  return `
    [out:json][timeout:60];
    (
      node["leisure"="pitch"]["name"](${bbox});
      way["leisure"="pitch"]["name"](${bbox});
      node["leisure"="sports_centre"]["name"](${bbox});
      way["leisure"="sports_centre"]["name"](${bbox});
      node["leisure"="stadium"]["name"](${bbox});
      way["leisure"="stadium"]["name"](${bbox});
    );
    out center tags;
  `.trim();
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function queryOverpass(query, attempt = 1) {
  const res = await fetch(OVERPASS_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      // Overpass (và WAF phía trước nó) từ chối request thiếu User-Agent —
      // Node's fetch không tự gửi header này như trình duyệt/curl.
      "User-Agent": "sportvenue-seed-script/1.0 (student project data collection)",
    },
    body: `data=${encodeURIComponent(query)}`,
  });

  if (res.status === 429 || res.status === 504) {
    if (attempt > MAX_RETRIES) {
      throw new Error(`Overpass trả về ${res.status} sau ${MAX_RETRIES} lần thử lại`);
    }
    const backoffMs = OVERPASS_DELAY_MS * attempt * 2;
    console.warn(`  Overpass ${res.status}, thử lại sau ${backoffMs / 1000}s (lần ${attempt}/${MAX_RETRIES})...`);
    await sleep(backoffMs);
    return queryOverpass(query, attempt + 1);
  }

  if (!res.ok) {
    throw new Error(`Overpass API lỗi ${res.status}: ${await res.text()}`);
  }

  return res.json();
}

function mapSportTag(rawSport) {
  if (!rawSport) return { mapped: null, raw: null };
  // tag "sport" trong OSM có thể là danh sách phân tách bởi dấu ";" (vd "soccer;tennis")
  const values = rawSport.split(";").map((v) => v.trim());
  const mapped = values.map((v) => SPORT_TAG_MAP[v]).find(Boolean) ?? null;
  return { mapped, raw: rawSport };
}

function buildAddress(tags, cityName) {
  const parts = [
    tags["addr:housenumber"],
    tags["addr:street"],
    tags["addr:suburb"] || tags["addr:district"],
    tags["addr:city"] || cityName,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(", ") : null; // null = cần tự bổ sung địa chỉ thủ công
}

function normalizeElement(element, cityName) {
  const tags = element.tags ?? {};
  const coords = element.type === "node"
    ? { lat: element.lat, lon: element.lon }
    : { lat: element.center?.lat ?? null, lon: element.center?.lon ?? null };

  const { mapped: sportTypeMapped, raw: sportRaw } = mapSportTag(tags.sport);

  return {
    osmId: `${element.type}/${element.id}`,
    name: tags.name,
    address: buildAddress(tags, cityName),
    latitude: coords.lat,
    longitude: coords.lon,
    sportTypeMapped, // khớp với sport_types.sport_name — null nghĩa là cần map tay
    sportRaw,         // giá trị gốc từ OSM, giữ lại để tham khảo khi map thủ công
    openingHoursRaw: tags.opening_hours ?? null,
    phone: tags.phone ?? tags["contact:phone"] ?? null,
    website: tags.website ?? tags["contact:website"] ?? null,
    city: cityName,
  };
}

async function fetchCity(city) {
  console.log(`Đang lấy dữ liệu cho ${city.name}...`);
  const query = buildQuery(city.bbox);
  const data = await queryOverpass(query);
  const venues = (data.elements ?? []).map((el) => normalizeElement(el, city.name));
  console.log(`  -> tìm được ${venues.length} địa điểm có tên.`);
  return venues;
}

async function main() {
  await mkdir(OUTPUT_DIR, { recursive: true });

  const allVenues = [];

  for (const city of CITIES) {
    const venues = await fetchCity(city);
    allVenues.push(...venues);

    const outFile = path.join(OUTPUT_DIR, `overpass-${city.slug}.json`);
    await writeFile(outFile, JSON.stringify(venues, null, 2), "utf-8");
    console.log(`  -> đã ghi ${outFile}`);

    // Nghỉ giữa các thành phố để không spam server free của Overpass.
    if (city !== CITIES[CITIES.length - 1]) {
      await sleep(OVERPASS_DELAY_MS);
    }
  }

  const combinedFile = path.join(OUTPUT_DIR, "overpass-all.json");
  await writeFile(combinedFile, JSON.stringify(allVenues, null, 2), "utf-8");

  const unmapped = allVenues.filter((v) => v.sportRaw && !v.sportTypeMapped).length;
  console.log(`\nTổng cộng: ${allVenues.length} địa điểm.`);
  console.log(`  - ${unmapped} địa điểm có tag "sport" nhưng chưa map được vào sport_types hiện có — xem field "sportRaw" để bổ sung SPORT_TAG_MAP nếu cần.`);
  console.log(`  - Đã ghi file tổng hợp: ${combinedFile}`);
}

main().catch((err) => {
  console.error("Script thất bại:", err.message);
  process.exit(1);
});
