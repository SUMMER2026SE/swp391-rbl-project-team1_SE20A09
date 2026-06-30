const { execSync } = require('child_process');
console.log(execSync('git log --all --oneline -n 20', { encoding: 'utf8' }));
