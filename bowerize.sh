#!/bin/sh

bower install

rm -rf web-app/images/ web-app/css/ web-app/js/
mkdir -p web-app/images/
mkdir -p web-app/css/
mkdir -p web-app/js/swagger-lib/

cp -R bower-sources/swagger-ui/dist/images/* web-app/images/
cp -R bower-sources/swagger-ui/dist/css/* web-app/css/
cp -R bower-sources/swagger-ui/dist/lib/* web-app/js/swagger-lib/
cp -R bower-sources/swagger-ui/dist/swagger-ui.js web-app/js/
