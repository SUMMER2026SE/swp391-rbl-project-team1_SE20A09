const fs = require('fs');
const dir = 'C:\\Users\\LAPTOP VINH HA\\.gemini\\antigravity\\brain\\19ee6c02-99ca-47a5-91ea-98e775a4e4bd\\.system_generated\\logs';
const files = fs.readdirSync(dir);
for (const file of files) {
  console.log(file);
}
