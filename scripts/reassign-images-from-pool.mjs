/**
 * Gán lại cover_image_url + gallery ảnh cho 161 complex ĐÃ seed (V7.6.1), dùng
 * pool ảnh Pexels đã mở rộng (scripts/output/pexels-images.json, sinh bởi
 * fetch-pexels-images.mjs bản v2 — scale theo số complex thực tế mỗi môn).
 *
 * Vì sao cần script riêng thay vì chỉ chạy lại generate-seed-sql-from-osm.mjs:
 * migration V7.6.1 đã áp dụng vào DB — Flyway không chạy lại migration cũ, nên
 * regenerate lại y hệt file cũ không có tác dụng gì với dữ liệu đã tồn tại.
 * Script này sinh 1 migration MỚI, UPDATE/DELETE+INSERT trên dữ liệu hiện có.
 *
 * KHÔNG cần kết nối DB — match theo (name, latitude, longitude), giống cách
 * scripts/enrich-address-from-coords.mjs đã làm cho phần địa chỉ.
 *
 * Chạy thử trước (không ghi migration, chỉ in ra + ghi preview):
 *   node scripts/reassign-images-from-pool.mjs --limit=5
 *
 * Chạy đầy đủ (ghi migration V8.2 thật):
 *   node scripts/reassign-images-from-pool.mjs
 *
 * Yêu cầu: đã chạy `PEXELS_API_KEY=xxx node scripts/fetch-pexels-images.mjs`
 * trước đó để có scripts/output/pexels-images.json bản mới (pool lớn hơn).
 */

import { readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const OVERPASS_FILE = path.join(process.cwd(), "scripts", "output", "overpass-all.json");
const PEXELS_FILE = path.join(process.cwd(), "scripts", "output", "pexels-images.json");
const OUTPUT_DIR = path.join(process.cwd(), "scripts", "output");
const MIGRATION_FILE = path.join(
  process.cwd(),
  "backend", "src", "main", "resources", "db", "migration",
  "V8.2__reassign_complex_images_from_expanded_pool.sql"
);

const SEEDED_SPORTS = new Set(["Football", "Badminton", "Basketball", "Tennis", "Volleyball"]);

function sqlEscape(value) {
  return String(value).replace(/'/g, "''");
}

/** Cho mỗi venue, chọn 1 bộ 3 ảnh PHÂN BIỆT NHAU bằng cách shuffle rồi lấy 3 phần tử đầu
 * (giống hệt pickImages() trong generate-seed-sql-from-osm.mjs) — đảm bảo không bao giờ trùng
 * ảnh trong cùng 1 complex; qua nhiều venue, random độc lập vẫn rải tương đối đều trên pool. */
function pickDistinctTriple(pool) {
  if (pool.length === 0) return [];
  const shuffled = [...pool].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, Math.min(3, pool.length));
}

async function main() {
  const limitArg = process.argv.find((a) => a.startsWith("--limit="));
  const limit = limitArg ? Number(limitArg.split("=")[1]) : null;
  const isDryRun = limit !== null;

  const [overpassRaw, pexelsRaw] = await Promise.all([
    readFile(OVERPASS_FILE, "utf-8"),
    readFile(PEXELS_FILE, "utf-8").catch(() => {
      throw new Error(
        `Không tìm thấy ${PEXELS_FILE} — chạy 'PEXELS_API_KEY=xxx node scripts/fetch-pexels-images.mjs' trước.`
      );
    }),
  ]);

  const allVenues = JSON.parse(overpassRaw);
  const pool = JSON.parse(pexelsRaw);

  const seeded = allVenues.filter((v) => v.sportTypeMapped && SEEDED_SPORTS.has(v.sportTypeMapped));
  console.log(`Tổng venue đã seed: ${seeded.length}.`);

  for (const sport of SEEDED_SPORTS) {
    const poolSize = pool[sport]?.length ?? 0;
    const venueCount = seeded.filter((v) => v.sportTypeMapped === sport).length;
    console.log(`  ${sport}: ${venueCount} complex, pool có ${poolSize} ảnh (mỗi ảnh dùng lại ~${(venueCount * 3 / Math.max(poolSize, 1)).toFixed(1)} lần).`);
  }

  const toProcess = isDryRun ? seeded.slice(0, limit) : seeded;
  if (isDryRun) console.log(`\n--- DRY RUN: chỉ xử lý ${toProcess.length} venue đầu tiên ---\n`);

  const results = [];
  for (const venue of toProcess) {
    const sport = venue.sportTypeMapped;
    const triple = pickDistinctTriple(pool[sport] ?? []);
    const [cover, g1, g2] = triple;
    if (!cover) {
      results.push({ name: venue.name, sport, status: "SKIPPED_NO_POOL" });
      continue;
    }
    results.push({
      name: venue.name,
      latitude: venue.latitude,
      longitude: venue.longitude,
      sport,
      coverImageUrl: cover.url,
      coverPhotographer: cover.photographer,
      galleryImageUrls: [g1, g2].filter(Boolean).map((p) => p.url),
      status: "OK",
    });
  }

  console.log(`\nKết quả: ${results.filter((r) => r.status === "OK").length} OK, ${results.filter((r) => r.status !== "OK").length} bị skip (pool rỗng cho môn đó).`);

  const previewFile = path.join(OUTPUT_DIR, isDryRun ? "image-reassignment-preview.json" : "image-reassignment.json");
  await writeFile(previewFile, JSON.stringify(results, null, 2), "utf-8");
  console.log(`Đã ghi ${previewFile}`);

  if (isDryRun) {
    console.log("\nDry run xong — review file preview rồi chạy lại KHÔNG kèm --limit để sinh migration thật.");
    return;
  }

  const sqlLines = [
    "-- V8.2__reassign_complex_images_from_expanded_pool.sql",
    "-- Sinh tự động bởi scripts/reassign-images-from-pool.mjs — thay ảnh cover/gallery",
    "-- của 161 complex seed từ V7.6.1 (pool cũ chỉ 15 ảnh/môn, Football dùng lại 1 ảnh",
    "-- ~21 lần vì có 106/161 complex) bằng pool Pexels đã mở rộng, scale theo đúng số",
    "-- complex thực tế mỗi môn (xem fetch-pexels-images.mjs bản v2).",
    "-- Vẫn là ảnh thật có giấy phép tự do (Pexels), KHÔNG phải ảnh chụp đúng địa điểm",
    "-- cụ thể (không có nguồn hợp pháp cho việc đó — xem V7.6.1).",
    "-- Match theo (name, latitude, longitude) — ổn định từ lúc seed, không cần complex_id.",
    "",
  ];
  for (const r of results.filter((x) => x.status === "OK")) {
    const whereClause = `name = '${sqlEscape(r.name)}' AND latitude = ${r.latitude} AND longitude = ${r.longitude}`;
    sqlLines.push(`UPDATE stadium_complexes SET cover_image_url = '${sqlEscape(r.coverImageUrl)}' WHERE ${whereClause};`);
    sqlLines.push(`DELETE FROM stadium_complex_images WHERE complex_id = (SELECT complex_id FROM stadium_complexes WHERE ${whereClause});`);
    if (r.galleryImageUrls.length > 0) {
      const arrayLiteral = r.galleryImageUrls.map((u) => `'${sqlEscape(u)}'`).join(", ");
      sqlLines.push(
        `INSERT INTO stadium_complex_images (complex_id, image_url) ` +
        `SELECT complex_id, unnest(ARRAY[${arrayLiteral}]) FROM stadium_complexes WHERE ${whereClause};`
      );
    }
    sqlLines.push("");
  }
  await writeFile(MIGRATION_FILE, sqlLines.join("\n"), "utf-8");
  console.log(`Đã ghi migration: ${MIGRATION_FILE} (${results.filter((r) => r.status === "OK").length} complex được gán lại ảnh).`);
}

main().catch((err) => {
  console.error("Script thất bại:", err.message);
  process.exit(1);
});
