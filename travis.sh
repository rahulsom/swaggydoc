#!/bin/bash
set -e
rm -rf *.zip
./bowerize.sh
./gradlew build grails2:gdocs

filename=$(find . -name "grails-*.zip" | head -1)
filename=$(basename $filename)

echo "Publishing plugin 'swaggydoc' version $version"

if [[ $TRAVIS_BRANCH == 'master' && $TRAVIS_REPO_SLUG == "rahulsom/swaggydoc" \
          && $TRAVIS_PULL_REQUEST == 'false' ]]; then
  git config --global user.name "$GIT_NAME"
  git config --global user.email "$GIT_EMAIL"
  git config --global credential.helper "store --file=~/.git-credentials"
  echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

  if [[ $filename != *-SNAPSHOT* ]]; then
    git clone https://${GH_TOKEN}@github.com/$TRAVIS_REPO_SLUG.git -b gh-pages \
        gh-pages --single-branch > /dev/null
    cd gh-pages
    git rm -rf .
    mkdir grails2
    cp -r ../grails2/build/docs/. .
    git add *
    git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID"
    git push origin HEAD
    cd ..
    rm -rf gh-pages
  else
    echo "SNAPSHOT version, not publishing docs"
  fi

  cat >> ~/.grails/settings.groovy << EOF
grails.project.repos.grailsCentral.username='${GRAILS_CENTRAL_USERNAME}'
grails.project.repos.grailsCentral.password='${GRAILS_CENTRAL_PASSWORD}'
EOF

  ./gradlew -PgrailsArgs='--no-scm --allow-overwrite --non-interactive' grails2:grails-publish-plugin
  ./gradlew grails3:bintrayUpload
else
  echo "Not on master branch, so not publishing"
  echo "TRAVIS_BRANCH: $TRAVIS_BRANCH"
  echo "TRAVIS_REPO_SLUG: $TRAVIS_REPO_SLUG"
  echo "TRAVIS_PULL_REQUEST: $TRAVIS_PULL_REQUEST"
fi
