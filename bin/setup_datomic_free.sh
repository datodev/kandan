#!/bin/bash

echo "The use of Datomic Free Edition is governed by the terms of the Datomic Free Edition License.\n"
echo "By downloading Datomic Free Edition, you are agreeing to the terms at at http://www.datomic.com/datomic-free-edition-license.html.\n"
echo "You MUST have agreed to the Datomic Free Edition license to use this script\n"

read -p "Have you read and agreed to the above license? [y/n] " -n 1 -r
echo    # (optional) move to a new line
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Aborting installation"
    exit 1
fi

source bin/datomic_version.sh
wget "-Odatomic-${VERSION}.zip" "https://my.datomic.com/downloads/free/${VERSION}"
unzip "-dtmp/" "datomic-${VERSION}.zip"
mv "tmp/datomic-free-${VERSION}" vendor/datomic
