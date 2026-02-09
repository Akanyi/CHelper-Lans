@REM 加载环境（假设已经下载好了emsdk并把emsdk目录加进了PATH环境变量）
echo ----- Loading environment -----
call emsdk_env.bat
call cd ..

@REM 编译Release模式
echo ----- [Release] configuring cmake project -----
call cmake -B cmake-build-emscripten-release -D CMAKE_BUILD_TYPE=Release -D CMAKE_TOOLCHAIN_FILE="%EMSDK%/upstream/emscripten/cmake/Modules/Platform/Emscripten.cmake" -G "Ninja"
call cd cmake-build-emscripten-release
echo ----- [Release] building cmake project -----
call cmake --build . --target CHelperWeb --parallel
echo ----- [Release] linking wasm  -----
call emcc libCHelperWeb.a libCHelperNoFilesystemCore.a 3rdparty/fmt/libfmt.a 3rdparty/spdlog/libspdlog.a 3rdparty/xxHash/cmake_unofficial/libxxhash.a -O3 -o libCHelperWeb.js -s FILESYSTEM=0 -s DISABLE_EXCEPTION_CATCHING=1 -s ALLOW_MEMORY_GROWTH -s ENVIRONMENT="web" -s EXPORTED_FUNCTIONS="['_init','_release','_onTextChanged','_onSelectionChanged','_getParamHint','_getErrorReasons','_getSuggestionSize','_getSuggestion','_getAllSuggestions','_onSuggestionClick','_getSyntaxTokens','_malloc','_free']" -s WASM=1 -s "EXPORTED_RUNTIME_METHODS=[]"
call cd ..

@REM 编译MinSizeRel模式
echo ----- [MinSizeRel] configuring cmake project -----
call cmake -B cmake-build-emscripten-minsizerel -D CMAKE_BUILD_TYPE=MinSizeRel -D CMAKE_TOOLCHAIN_FILE="%EMSDK%/upstream/emscripten/cmake/Modules/Platform/Emscripten.cmake" -G "Ninja"
call cd cmake-build-emscripten-minsizerel
echo ----- [MinSizeRel] building cmake project -----
call cmake --build . --target CHelperWeb --parallel
echo ----- [MinSizeRel] linking wasm -----
call emcc libCHelperWeb.a libCHelperNoFilesystemCore.a 3rdparty/fmt/libfmt.a 3rdparty/spdlog/libspdlog.a 3rdparty/xxHash/cmake_unofficial/libxxhash.a -Os -o libCHelperWeb.js -s FILESYSTEM=0 -s DISABLE_EXCEPTION_CATCHING=1 -s ALLOW_MEMORY_GROWTH -s ENVIRONMENT="web" -s EXPORTED_FUNCTIONS="['_init','_release','_onTextChanged','_onSelectionChanged','_getParamHint','_getErrorReasons','_getSuggestionSize','_getSuggestion','_getAllSuggestions','_onSuggestionClick','_getSyntaxTokens','_malloc','_free']" -s WASM=1 -s "EXPORTED_RUNTIME_METHODS=[]"
call cd ..

@REM 修改JS胶水代码
echo ----- patching libCHelperWeb.js -----
call python ./script/patch-wasm.py
