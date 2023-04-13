#!/bin/bash

pandoc \
  -t html5 \
  --columns 1000 \
  --css etc/doc.css \
  -s \
  getting-started.md \
  -o getting-started.html

wkhtmltopdf \
  --enable-local-file-access \
  --header-html etc/header.html \
  --footer-html etc/footer.html \
  --footer-line \
  toc \
  --toc-text-size-shrink 1 \
  getting-started.html getting-started.pdf