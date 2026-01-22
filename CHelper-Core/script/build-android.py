import os
import subprocess
import sys
import zipfile
from urllib.request import urlretrieve

if sys.platform == "win32":
    CMAKE_PATH = "D:/AS/sdk/cmake/3.22.1/bin/cmake.exe"
    NINJA_PATH = "D:/AS/sdk/cmake/3.22.1/bin/ninja.exe"
else:
    CMAKE_PATH = "/opt/android-sdk/cmake/3.22.1/bin/cmake"
    NINJA_PATH = "/opt/android-sdk/cmake/3.22.1/bin/ninja"

os.chdir('../..')
CURRENT_DIR = os.getcwd()
CMAKE_PATH = 'cmake'
NDK_VERSION = 'r29'
NDK_PATH = os.path.join(CURRENT_DIR, f'android-ndk-{NDK_VERSION}')

def download_android_ndk():
    if os.path.exists(NDK_PATH):
        return
    if sys.platform == "win32":
        ndk_url = f"https://googledownloads.cn/android/repository/android-ndk-{NDK_VERSION}-windows.zip"
        archive_path = os.path.join(CURRENT_DIR, f"android-ndk-{NDK_VERSION}-windows.zip")
    else:
        ndk_url = f"https://googledownloads.cn/android/repository/android-ndk-{NDK_VERSION}-linux.zip"
        archive_path = os.path.join(CURRENT_DIR, f"android-ndk-{NDK_VERSION}-linux.zip")
    print(f"Downloading Android NDK from {ndk_url}")
    print("This may take several minutes...")
    try:
        def progress_callback(count, block_size, total_size):
            percent = int(count * block_size * 100 / total_size)
            sys.stdout.write(f"\rDownloading... {percent}%")
            sys.stdout.flush()
        urlretrieve(ndk_url, archive_path, progress_callback)
        print("\nDownload completed!")
        print("Extracting NDK...")
        if sys.platform == "win32":
            with zipfile.ZipFile(archive_path, 'r') as zip_ref:
                zip_ref.extractall(CURRENT_DIR)
        else:
            with zipfile.ZipFile(archive_path, 'r') as zip_ref:
                zip_ref.extractall(CURRENT_DIR)
        os.remove(archive_path)
        print(f"NDK successfully installed to {NDK_PATH}")
    except Exception as e:
        print(f"Failed to download or extract NDK: {e}")
        print("\n--- Alternative Download Method ---")
        print("1. Manually download Android NDK:")
        print(f"   {ndk_url}")
        print(f"2. Extract it to: {CURRENT_DIR}")
        print(f"3. Ensure the folder is named: android-ndk-{NDK_VERSION}")
        sys.exit(1)

def build_android_core():
    build_directory = "./build/android_core"
    configure_cmd = f"""{CMAKE_PATH} \
-S ./CHelper-Core \
-D CMAKE_BUILD_TYPE=Release \
-D CMAKE_TOOLCHAIN_FILE={NDK_PATH}/build/cmake/android.toolchain.cmake \
-D ANDROID_ABI=arm64-v8a \
-D ANDROID_NDK={NDK_PATH} \
-D ANDROID_PLATFORM=android-24 \
-D CMAKE_ANDROID_ARCH_ABI=arm64-v8a \
-D CMAKE_ANDROID_NDK={NDK_PATH} \
-D CMAKE_EXPORT_COMPILE_COMMANDS=ON \
-D CMAKE_MAKE_PROGRAM={NINJA_PATH} \
-D CMAKE_SYSTEM_NAME=Android \
-D CMAKE_SYSTEM_VERSION=24 \
-B {build_directory} \
-G Ninja"""

    subprocess.run(configure_cmd, check=True)
    subprocess.run(f"{CMAKE_PATH} --build {build_directory} --target CHelperAndroid", check=True)
    so_file = f"{build_directory}/libCHelperAndroid.so"
    if sys.platform == "win32":
        llvm_strip_path = f"{NDK_PATH}/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-strip.exe"
    else:
        llvm_strip_path = f"{NDK_PATH}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"
    strip_cmd = f"{llvm_strip_path} {so_file} -o {so_file}.striped"
    subprocess.run(strip_cmd, shell=True)

if __name__ == "__main__":
    download_android_ndk()
    build_android_core()
