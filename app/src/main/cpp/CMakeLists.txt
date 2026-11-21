cmake_minimum_required(VERSION 3.22.1)
project("pdfmaker")

set(CPP_DIR "${CMAKE_CURRENT_SOURCE_DIR}")

# 1. Kpathsea source files
file(GLOB KPATHSEA_SOURCES "${CPP_DIR}/kpathsea/*.c")

# 2. Core Web2c runtime and translated C files (Explicitly selected)
set(WEB2C_SOURCES
        "${CPP_DIR}/pdftex/built_engines/pdftexini.c"
        "${CPP_DIR}/pdftex/built_engines/pdftex0.c"
        "${CPP_DIR}/pdftex/built_engines/pdftex-pool.c"
)
#file(GLOB SYNCTEX_SOURCES
#        "${CPP_DIR}/web2c/synctexdir/*.c"
#)

## 3. MD5 utility sources (Required by pdftexdir/utils.c)
#file(GLOB MD5_SOURCES "${CPP_DIR}/web2c/libmd5/*.c")

# 4. PdfTeX directory extensions (C and C++)
file(GLOB PDFTEX_SOURCES
        "${CPP_DIR}/pdftex/pdftex_src/*.cc"
        "${CPP_DIR}/pdftex/pdftex_src/*.c"
        "${CPP_DIR}/pdftex/web2c_built/*.c"
)

file(GLOB LIBPNG_SOURCES
        "${CPP_DIR}/pdftex/libs/libpng_src/*.c"
)

file(GLOB XPDF_SOURCES
        "${CPP_DIR}/pdftex/libs/xpdf/src/*.c"
        "${CPP_DIR}/pdftex/libs/xpdf/src/*.cc"
)

file(GLOB WEB2CSRC_SOURCES
        "${CPP_DIR}/pdftex/web2c_src/lib/*c"
        "${CPP_DIR}/pdftex/web2c_src/libmd5/*c"
        "${CPP_DIR}/pdftex/web2c_src/synctexdir/*c"
)

# Enforce standard definitions required by Bionic and Kpathsea headers
add_definitions(-DHAVE_CONFIG_H -D_GNU_SOURCE -DMAKE_KPSE_DLL -D__ANDROID__=1 -Wl,--error-limit=0)

# Include directories
include_directories(BEFORE
        ${CPP_DIR}/kpathsea
        ${CPP_DIR}/pdftex/web2c_built
        ${CPP_DIR}/pdftex/pdftex_src
        ${CPP_DIR}
        ${CPP_DIR}/pdftex/web2c_built/w2c
        ${CPP_DIR}/pdftex
        ${CPP_DIR}/pdftex/libs
        ${CPP_DIR}/pdftex/libs/xpdf
        ${CPP_DIR}/pdftex/libs/xpdf/src
        ${CPP_DIR}/pdftex/libs/xpdf/built
        ${CPP_DIR}/pdftex/libs/libpng_src
        ${CPP_DIR}/pdftex/web2c_src
        ${CPP_DIR}/pdftex/web2c_src/lib
        ${CPP_DIR}/pdftex/web2c_src/w2c
        ${CPP_DIR}/pdftex/web2c_src/synctexdir
        ${CPP_DIR}/pdftex/web2c_src/libmd5
        ${CPP_DIR}/pdftex/built_engines
)

list(FILTER XPDF_SOURCES EXCLUDE REGEX
        "/(pdftotext|pdftopng|pdftohtml|pdftoppm|pdftops|pdffonts|pdfinfo|pdfimages|pdfdetach)\\.cc?$"
)

list(FILTER WEB2CSRC_SOURCES EXCLUDE REGEX
        "/(makecpool|main)\\.c$"
)

list(FILTER PDFTEX_SOURCES EXCLUDE REGEX
        "/macnames\\.c$"
)


# Define the standalone executable
#add_executable(
#        pdflatex
#        ${KPATHSEA_SOURCES}
#        ${CPP_DIR}/pdftex/texmfmp.c
#        ${LIBPNG_SOURCES}
#        ${WEB2CSRC_SOURCES}
#        ${WEB2C_SOURCES}
#        ${XPDF_SOURCES}
#        ${PDFTEX_SOURCES}
#)


#target_compile_definitions(pdflatex PRIVATE
#        pdfTeX=1
#        __SyncTeX__=1
#        TeX=1
#        SYNCTEX_ENGINE_H="synctex-pdftex.h"
#        PNG_ARM_NEON_OPT=0
#)

# Compiler options
#target_compile_options(pdflatex PRIVATE
#        -O3
#        -fPIC
#        -Wall
#        -Wno-unused-variable
#        -Wno-unused-function
#)

# Link against system libraries, compression, and logging
#target_link_libraries(
#        pdflatex
#        m
#        z
#        log
#)