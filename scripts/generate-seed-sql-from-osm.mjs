/**
 * Chuyển dữ liệu OSM đã cào (scripts/output/overpass-all.json) thành file
 * Flyway migration seed, theo đúng cấu trúc 3 tầng StadiumComplex -> Stadium
 * (FACILITY) -> Stadium (COURT) đã có từ V7.1/V7.2.
 *
 * Chạy sau khi đã có scripts/output/overpass-all.json:
 *   node scripts/generate-seed-sql-from-osm.mjs
 *
 * Mỗi địa điểm được sinh thành 1 khối SQL độc lập dùng chuỗi CTE
 * (INSERT ... RETURNING) để nối complex -> facility -> courts -> time_slots
 * mà không cần tra cứu lại theo tên (tránh đụng độ khi có tên trùng nhau).
 */

import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const INPUT_FILE = path.join(process.cwd(), "scripts", "output", "overpass-all.json");
const PEXELS_FILE = path.join(process.cwd(), "scripts", "output", "pexels-images.json");
// LƯU Ý: số version Flyway (V7.6) không tự động — mỗi lần regenerate, hãy
// kiểm tra lại trong backend/.../db/migration/ (và các branch khác đang mở
// PR) xem version này có bị trùng không trước khi chạy migrate. Từng bị vỡ
// vì trùng V7.4/V7.5 với 1 branch khác (feature/stadium/maintenance-schedule)
// đã áp lên chung 1 DB dev — Flyway coi version đã chạy nên bỏ qua file mới.
const OUTPUT_FILE = path.join(
  process.cwd(),
  "backend", "src", "main", "resources", "db", "migration",
  "V7.6__seed_real_stadiums_osm.sql"
);

// 2 owner đã APPROVED sẵn có trong V2__seed_data.sql — luân phiên gán chủ sân.
const OWNERS = ["Sport Venue Owner Corp", "Huy Sport Center"];

// Cấu hình theo môn: khoảng giá/giờ (VND) và số sân con (courts) mỗi facility.
const SPORT_CONFIG = {
  Football: { priceMin: 300000, priceMax: 600000, courtsMin: 1, courtsMax: 2 },
  Badminton: { priceMin: 60000, priceMax: 120000, courtsMin: 2, courtsMax: 4 },
  Basketball: { priceMin: 150000, priceMax: 300000, courtsMin: 1, courtsMax: 2 },
  Tennis: { priceMin: 150000, priceMax: 300000, courtsMin: 1, courtsMax: 2 },
  Volleyball: { priceMin: 100000, priceMax: 200000, courtsMin: 1, courtsMax: 2 },
};

const FOOTBALL_FIELD_TYPES = ["FIVE_A_SIDE", "SEVEN_A_SIDE", "ELEVEN_A_SIDE"];

const OPEN_TIME = "06:00";
const CLOSE_TIME = "22:00";

function sqlEscape(value) {
  return String(value).replace(/'/g, "''");
}

function sqlString(value) {
  return value === null || value === undefined ? "NULL" : `'${sqlEscape(value)}'`;
}

function randInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function roundToThousand(n) {
  return Math.round(n / 1000) * 1000;
}

// Rút ngẫu nhiên n ảnh không trùng nhau từ pool ảnh thật (Pexels) của 1 môn thể thao.
function pickImages(pool, sport, n) {
  const candidates = pool[sport] ?? [];
  const shuffled = [...candidates].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, n).map((p) => p.url);
}

function buildDescription(venue) {
  const bits = ["Dữ liệu thực tế tổng hợp từ OpenStreetMap."];
  if (venue.website) bits.push(`Website: ${venue.website}`);
  return bits.join(" ");
}

function buildComplexBlock(venue, index, imagePool) {
  const owner = OWNERS[index % OWNERS.length];
  const cfg = SPORT_CONFIG[venue.sportTypeMapped];
  const basePrice = roundToThousand(randInt(cfg.priceMin, cfg.priceMax));
  const courtCount = randInt(cfg.courtsMin, cfg.courtsMax);
  // KHÔNG random average_rating/review_count ở đây — để mặc định (5.0 / 0).
  // V7.7 (seed review) là nguồn duy nhất được phép ghi đè 2 field này, dựa
  // trên review thật. Random ở bước tạo complex từng gây bug: complex nào
  // không có court được chọn review ở V7.7 (~20% theo thiết kế) vẫn giữ
  // nguyên rating/review_count GIẢ — hiển thị "4.3 sao" trong khi có 0 review
  // thật, gây khó hiểu cho người dùng.
  const pickedImages = pickImages(imagePool, venue.sportTypeMapped, 3);
  const coverImage = pickedImages[0] ?? `https://picsum.photos/seed/${encodeURIComponent(venue.osmId)}/800/600`;
  const galleryImages = pickedImages.slice(1);
  const description = buildDescription(venue);

  const lines = [];
  lines.push(`-- ${venue.name} (${venue.city}) — osm:${venue.osmId}`);
  lines.push(`WITH new_complex AS (`);
  lines.push(`    INSERT INTO stadium_complexes (`);
  lines.push(`        owner_id, name, description, address, phone, latitude, longitude,`);
  lines.push(`        cover_image_url, complex_status, approved_status, average_rating, review_count, created_at`);
  lines.push(`    ) VALUES (`);
  lines.push(`        (SELECT owner_id FROM owners WHERE business_name = ${sqlString(owner)}),`);
  lines.push(`        ${sqlString(venue.name)},`);
  lines.push(`        ${sqlString(description)},`);
  lines.push(`        ${sqlString(venue.address)},`);
  lines.push(`        ${sqlString(venue.phone)},`);
  lines.push(`        ${venue.latitude ?? "NULL"}, ${venue.longitude ?? "NULL"},`);
  lines.push(`        ${sqlString(coverImage)}, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()`);
  lines.push(`    ) RETURNING complex_id`);
  lines.push(`),`);
  if (galleryImages.length > 0) {
    lines.push(`gallery_images AS (`);
    lines.push(`    INSERT INTO stadium_complex_images (complex_id, image_url)`);
    lines.push(`    SELECT complex_id, unnest(ARRAY[${galleryImages.map(sqlString).join(", ")}])`);
    lines.push(`    FROM new_complex`);
    lines.push(`),`);
  }
  lines.push(`new_facility AS (`);
  lines.push(`    INSERT INTO stadiums (`);
  lines.push(`        owner_id, sport_type_id, stadium_name, description, node_type, complex_id,`);
  lines.push(`        open_time, close_time, price_per_hour, stadium_status, approved_status,`);
  lines.push(`        average_rating, review_count, created_at`);
  lines.push(`    )`);
  // owner_id PHẢI được set trực tiếp trên Stadium (không chỉ ở complex) — đúng
  // như StadiumServiceImpl.java thật đang làm khi tạo Facility/Court. Nhiều
  // repository (ReviewRepository, BookingRepository, ComplaintRepository,
  // PaymentRepository, OwnerBookingService.validateStadiumOwnership) query
  // trực tiếp qua "stadium.owner", KHÔNG dùng resolveOwner() — để NULL sẽ
  // khiến Owner không thấy booking/review/complaint/payment của sân này.
  lines.push(`    SELECT (SELECT owner_id FROM owners WHERE business_name = ${sqlString(owner)}),`);
  lines.push(`        (SELECT sport_type_id FROM sport_types WHERE sport_name = ${sqlString(venue.sportTypeMapped)}),`);
  lines.push(`        ${sqlString(venue.name)}, ${sqlString(description)}, 'FACILITY', complex_id,`);
  lines.push(`        '${OPEN_TIME}', '${CLOSE_TIME}', ${basePrice}.00, 'AVAILABLE', 'APPROVED', 5.0, 0, NOW()`);
  lines.push(`    FROM new_complex`);
  lines.push(`    RETURNING stadium_id, complex_id`);
  lines.push(`),`);
  lines.push(`new_courts AS (`);
  lines.push(`    INSERT INTO stadiums (`);
  lines.push(`        owner_id, stadium_name, node_type, complex_id, parent_stadium_id, open_time, close_time,`);
  lines.push(`        price_per_hour, football_field_type, stadium_status, approved_status,`);
  lines.push(`        average_rating, review_count, created_at`);
  lines.push(`    )`);
  lines.push(`    SELECT (SELECT owner_id FROM owners WHERE business_name = ${sqlString(owner)}),`);
  lines.push(`        'Sân ' || court_no, 'COURT', f.complex_id, f.stadium_id, '${OPEN_TIME}', '${CLOSE_TIME}',`);
  lines.push(`        ${basePrice}.00 + (court_no * 10000),`);
  if (venue.sportTypeMapped === "Football") {
    lines.push(`        (ARRAY['${FOOTBALL_FIELD_TYPES.join("','")}'])[1 + floor(random() * ${FOOTBALL_FIELD_TYPES.length})::int],`);
  } else {
    lines.push(`        NULL,`);
  }
  lines.push(`        'AVAILABLE', 'APPROVED', 5.0, 0, NOW()`);
  lines.push(`    FROM new_facility f, generate_series(1, ${courtCount}) AS court_no`);
  lines.push(`    RETURNING stadium_id`);
  lines.push(`),`);
  lines.push(`link_sport_type AS (`);
  lines.push(`    INSERT INTO complex_sport_types (complex_id, sport_type_id)`);
  lines.push(`    SELECT complex_id, (SELECT sport_type_id FROM sport_types WHERE sport_name = ${sqlString(venue.sportTypeMapped)})`);
  lines.push(`    FROM new_complex`);
  lines.push(`    ON CONFLICT DO NOTHING`);
  lines.push(`)`);
  lines.push(`INSERT INTO time_slots (stadium_id, start_time, end_time, price_per_slot, slot_status)`);
  lines.push(`SELECT stadium_id, make_time(h, 0, 0), make_time(h + 1, 0, 0), ${basePrice}.00, 'AVAILABLE'`);
  lines.push(`FROM new_courts, generate_series(6, 21) AS h;`);
  lines.push("");
  return lines.join("\n");
}

async function loadImagePool() {
  try {
    const raw = await readFile(PEXELS_FILE, "utf-8");
    return JSON.parse(raw);
  } catch {
    console.warn(`Không tìm thấy ${PEXELS_FILE} — chạy "node scripts/fetch-pexels-images.mjs" trước để có ảnh thật. Dùng picsum.photos tạm thời.`);
    return null;
  }
}

async function main() {
  const raw = await readFile(INPUT_FILE, "utf-8");
  const venues = JSON.parse(raw).filter((v) => v.sportTypeMapped && SPORT_CONFIG[v.sportTypeMapped]);
  const imagePool = await loadImagePool();

  const header = [
    "-- ══════════════════════════════════════════════════════════════════════════",
    "-- V7.6__seed_real_stadiums_osm.sql — Seed sân thật từ OpenStreetMap",
    "-- Nguồn: scripts/fetch-overpass-stadiums.mjs (Overpass API, dữ liệu OSM/ODbL)",
    `-- Sinh tự động bởi scripts/generate-seed-sql-from-osm.mjs — ${venues.length} địa điểm`,
    "-- Giá thuê/giờ, giờ mở cửa, rating là dữ liệu GIẢ LẬP (OSM không có) — chỉ",
    "-- tên, địa chỉ, toạ độ, môn thể thao là thật.",
    "-- Ảnh (cover_image_url, stadium_complex_images) là ảnh stock thật lấy từ",
    "-- Pexels API (scripts/fetch-pexels-images.mjs) theo môn thể thao — KHÔNG",
    "-- phải ảnh chụp đúng địa điểm cụ thể đó (không có nguồn hợp pháp cho việc này).",
    "-- ══════════════════════════════════════════════════════════════════════════",
    "",
  ].join("\n");

  const blocks = venues.map((venue, index) => buildComplexBlock(venue, index, imagePool ?? {}));

  await writeFile(OUTPUT_FILE, header + blocks.join("\n"), "utf-8");
  console.log(`Đã sinh ${venues.length} địa điểm vào ${OUTPUT_FILE}`);
}

main().catch((err) => {
  console.error("Script thất bại:", err.message);
  process.exit(1);
});
