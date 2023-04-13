#!/bin/bash

pandoc \
  -t html5 \
  --columns 1000 \
  --css etc/doc.css \
  -s \
  api.md \
  -o api.html

wkhtmltopdf \
  --enable-local-file-access \
  --header-html etc/header.html \
  --footer-html etc/footer.html \
  --footer-line \
  toc \
  --toc-text-size-shrink 1 \
  api.html api.pdf