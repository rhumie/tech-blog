#!/usr/bin/env bash

# Update System Packages.
sudo apt-get update

# Install gnupg2 for sharing Git credentials.
# cf. https://code.visualstudio.com/remote/advancedcontainers/sharing-git-credentials#_sharing-gpg-keys
sudo apt-get install -y gnupg2
sudo apt-get install -y python3

# Set the owner of the directories mounted via the volumes.
sudo chown vscode:vscode node_modules
sudo chown vscode:vscode "$HOME/.claude"

# Install npm dependencies.
npm install -g npm
npm install

# Install uv.
curl -LsSf https://astral.sh/uv/0.12.15/install.sh | sh

# Enable SDKMAN's per-directory switching, so that entering a directory that
# holds a .sdkmanrc activates the JDK pinned there.
sed -i "s/^sdkman_auto_env=.*/sdkman_auto_env=true/" "$SDKMAN_DIR/etc/config"

# Set aliases
echo "alias ll=\"ls -l\"" >> "$HOME/.bash_aliases"
