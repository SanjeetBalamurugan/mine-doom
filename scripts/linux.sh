sudo dpkg --add-architecture i386
sudo apt update

sudo apt install -y \
  build-essential \
  cmake ninja-build git curl unzip zip \
  openjdk-21-jdk \
  gcc-mingw-w64-x86-64 \
  gcc-mingw-w64-i686 \
  gcc-aarch64-linux-gnu \
  gcc-arm-linux-gnueabihf \
  libc6-dev-arm64-cross \
  libc6-dev-armhf-cross \
  clang lld llvm