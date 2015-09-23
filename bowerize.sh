#!/bin/sh

bower install

function resetDirs() {
  def GRH=$1
  if [ "$GRH" = "" ];
    echo "No dir specified"
    exit 1
  fi
  rm -rf $GRH/web-app/images/ $GRH/web-app/css/ $GRH/web-app/js/
  mkdir -p $GRH/web-app/images/
  mkdir -p $GRH/web-app/css/
  mkdir -p $GRH/web-app/js/swagger-lib/

  cp -R bower-sources/swagger-ui/dist/images/*      $GRH/web-app/images/
  cp -R bower-sources/swagger-ui/dist/css/*         $GRH/web-app/css/
  cp -R bower-sources/swagger-ui/dist/lib/*         $GRH/web-app/js/swagger-lib/
  cp -R bower-sources/swagger-ui/dist/swagger-ui.js $GRH/web-app/js/

}

resetDirs grails2
resetDirs grails3
