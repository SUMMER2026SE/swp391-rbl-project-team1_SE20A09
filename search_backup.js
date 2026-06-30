const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

try {
  // Use ripgrep or findstr to search for a unique string in the 1080-line file
  const out = execSync('findstr /s /i /m "setHoveredMsgId" "D:\\PE PRJ301\\STADIUM\\swp391-rbl-project-team1_SE20A09\\*"', { encoding: 'utf8', maxBuffer: 10*1024*1024 });
  console.log("Found in:");
  console.log(out);
} catch (e) {
  console.log("Not found or error:", e.message);
}
