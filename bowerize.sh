#!/bin/sh

bower install

BOWER_DIR=bower-sources/swagger-ui/dist/

WA_DIR=grails2/web-app

rm -rf $WA_DIR/images/ $WA_DIR/css/ $WA_DIR/js/
mkdir -p $WA_DIR/images/
mkdir -p $WA_DIR/css/
mkdir -p $WA_DIR/js/swagger-lib/

cp -R $BOWER_DIR/images/*      $WA_DIR/images/
cp -R $BOWER_DIR/css/*         $WA_DIR/css/
cp -R $BOWER_DIR/lib/*         $WA_DIR/js/swagger-lib/
cp -R $BOWER_DIR/swagger-ui.js $WA_DIR/js/

ASSETS=swaggydoc-grails3/grails-app/assets
rm -rf $ASSETS/images/ $ASSETS/stylesheets/ $ASSETS/javascripts/
mkdir -p $ASSETS/images/
mkdir -p $ASSETS/stylesheets/
mkdir -p $ASSETS/javascripts/swagger-lib/

cp -R $BOWER_DIR/images/*      $ASSETS/images/
cp -R $BOWER_DIR/css/*         $ASSETS/stylesheets/
cp -R $BOWER_DIR/lib/*         $ASSETS/javascripts/swagger-lib/
cp -R $BOWER_DIR/swagger-ui.js $ASSETS/javascripts/
