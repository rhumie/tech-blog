#!/usr/bin/env bash

# Update System Packages.
sudo apt-get update

# Install gnupg2 for sharing Git credentials.
# cf. https://code.visualstudio.com/remote/advancedcontainers/sharing-git-credentials#_sharing-gpg-keys
sudo apt-get install -y gnupg2

# Set the owner of the directories mounted via the volumes.
sudo chown vscode:vscode node_modules

# Install npm dependencies.
npm install -g npm
npm install

# Set aliases
echo "alias ll=\"ls -l\"" >> "$HOME/.bash_aliases"
