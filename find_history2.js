const fs = require('fs');
const path = require('path');
const historyDir = path.join(process.env.APPDATA, 'Code', 'User', 'History');

function search(dir, results) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const fullPath = path.join(dir, file);
    const stat = fs.statSync(fullPath);
    if (stat.isDirectory()) {
      search(fullPath, results);
    } else if (stat.isFile() && file !== 'entries.json') {
      results.push({ path: fullPath, mtime: stat.mtimeMs });
    }
  }
}

const results = [];
search(historyDir, results);
results.sort((a, b) => b.mtime - a.mtime);
for (const res of results.slice(0, 50)) {
  console.log(res.path, new Date(res.mtime).toISOString());
}
