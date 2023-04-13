#!/bin/bash

rm -rf ./conf
mkdir ./conf

cat >>./conf/nginx.conf <<EOL
server {
    listen       5500;
    server_name  localhost;

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        expires -1;
    }

    location ^~ /gate/ {
        proxy_pass http://127.0.0.1:${CNFT_GATE_PORT}/;
        proxy_pass_request_headers      on;

    }

}
EOL
