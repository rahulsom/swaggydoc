#!/bin/bash

OLDVER=$(cat examples/swaggydoc-grails2-example/grails-app/conf/BuildConfig.groovy | grep :swaggydoc: | sed -e "s/.*://g" | sed -e 's/"//g')
NEWVER=$(cat build.gradle | grep version | sed -e "s/-.*//g" | sed -e 's/.*"//g')
SNAPSHOT=${_SNAPSHOT:-$NEWVER-SNAPSHOT}

echo "Old Version: $OLDVER"
echo "New Version: $NEWVER"
echo "Snapshot:    $SNAPSHOT"

EXAMPLE2=examples/swaggydoc-grails2-example/grails-app/conf/BuildConfig.groovy
sed -i '' "s/swaggydoc-commons:$OLDVER/swaggydoc-commons:$NEWVER/" $EXAMPLE2
sed -i '' "s/swaggydoc:$OLDVER/swaggydoc:$NEWVER/"                 $EXAMPLE2

EXAMPLE3=examples/swaggydoc-grails3-example/build.gradle
sed -i '' "s/swaggydoc-grails3:$OLDVER/swaggydoc-grails3:$NEWVER/" $EXAMPLE3

BUILD2=grails2/grails-app/conf/BuildConfig.groovy
sed -i '' "s/swaggydoc-commons:$OLDVER/swaggydoc-commons:$NEWVER/" $BUILD2

DOC2=grails2/src/docs/guide/installation.gdoc
sed -i '' "s/swaggydoc-commons:$OLDVER/swaggydoc-commons:$NEWVER/" $DOC2
sed -i '' "s/swaggydoc:$OLDVER/swaggydoc:$NEWVER/"                 $DOC2
sed -i '' "s/swaggydoc-grails3:$OLDVER/swaggydoc-grails3:$NEWVER/" $DOC2

BUILD_TOP=build.gradle
sed -i '' "s/version \"$SNAPSHOT\"/version \"$NEWVER\"/"           $BUILD_TOP

PLUGIN2=grails2/SwaggydocGrailsPlugin.groovy
sed -i '' "s/version = \"$SNAPSHOT\"/version = \"$NEWVER\"/"       $PLUGIN2
