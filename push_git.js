const { exec } = require('child_process');

console.log("Adding changes...");
exec('git add .', (err, stdout, stderr) => {
    if (err) { console.error("Add Error:", stderr); return; }
    
    console.log("Committing changes...");
    exec('git commit -m "fix: resolve conflict and complete chat features"', (err2, stdout2, stderr2) => {
        // Ignore commit error if there's nothing to commit
        console.log("Pushing forcefully...");
        exec('git push -f origin HEAD:feature/huyhoang1233/fix-chat-messaging', (err3, stdout3, stderr3) => {
            if (err3) {
                console.error("Push Error:", stderr3);
            } else {
                console.log("Push Success!", stdout3);
            }
        });
    });
});
