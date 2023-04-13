#!/bin/bash

pandoc \
  -t html5 \
  --columns 1000 \
  --css etc/doc.css \
  -s \
  sber-chain-platform.md \
  -o doc.html

wkhtmltopdf \
  --enable-local-file-access \
  --header-html etc/header.html \
  --footer-html etc/footer.html \
  --footer-line \
  toc \
  --toc-text-size-shrink 1 \
  doc.html sber-chain-platform.pdf