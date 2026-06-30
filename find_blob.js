const fs = require('fs');
const path = require('path');
const dir = 'd:\\PE PRJ301\\STADIUM\\swp391-rbl-project-team1_SE20A09\\.git\\lost-found\\other';
if (!fs.existsSync(dir)) {
  console.log('No lost-found dir');
  process.exit(0);
}
const files = fs.readdirSync(dir);
for (const file of files) {
  const stat = fs.statSync(path.join(dir, file));
  if (stat.size > 40000 && stat.size < 60000) {
    console.log(file, stat.size);
  }
}
