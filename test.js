const http = require('http');

const data = JSON.stringify({
  email: 'owner@sportvenue.com',
  password: 'password123'
});

const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/v1/auth/login',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
};

const req = http.request(options, res => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => {
    try {
        const token = JSON.parse(body).accessToken;
        if (!token) throw new Error("No token");
        console.log("Got token.");
        
        // Now try to approve booking 2
        const putReq = http.request({
            hostname: 'localhost',
            port: 8080,
            path: '/api/v1/owner/bookings/2/confirm',
            method: 'PUT',
            headers: { 'Authorization': 'Bearer ' + token }
        }, putRes => {
            let putBody = '';
            putRes.on('data', d => putBody += d);
            putRes.on('end', () => {
                console.log("PUT status:", putRes.statusCode);
                console.log("PUT response:", putBody);
            });
        });
        putReq.end();
    } catch(e) {
        console.log("Login body:", body);
    }
  });
});

req.on('error', error => console.error(error));
req.write(data);
req.end();
