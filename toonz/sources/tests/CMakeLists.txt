set(CMAKE_INCLUDE_CURRENT_DIR ON)

add_executable(tlv_roundtrip_test tlv_roundtrip_test.cpp)

target_include_directories(tlv_roundtrip_test PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../include
)

set(TEST_TOONZ_LIBS tnzcore tnzbase tnzext toonzlib image sound)

if(BUILD_ENV_MSVC)
    target_link_libraries(tlv_roundtrip_test Qt5::Core Qt5::Gui ${TEST_TOONZ_LIBS})
else()
    _find_toonz_library(TEST_LIBS "tnzcore;tnzbase;tnzext;toonzlib;image;sound")
    target_link_libraries(tlv_roundtrip_test Qt5::Core Qt5::Gui ${TEST_LIBS})
    add_dependencies(tlv_roundtrip_test ${TEST_TOONZ_LIBS})
endif()

# The Toonz shared libraries are built with @executable_path install names, so
# they have to sit next to the test binary rather than on a search path.
if(NOT BUILD_ENV_MSVC)
    foreach(_lib ${TEST_TOONZ_LIBS})
        add_custom_command(TARGET tlv_roundtrip_test POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    $<TARGET_FILE:${_lib}> $<TARGET_FILE_DIR:tlv_roundtrip_test>
            VERBATIM
        )
    endforeach()
endif()

# The TLV codec compresses by invoking the lzocompress and lzodecompress
# helpers from the running executable's own directory, so the test needs them
# alongside it exactly as the application bundle does.
foreach(_tool lzocompress lzodecompress)
    add_custom_command(TARGET tlv_roundtrip_test POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
                $<TARGET_FILE:${_tool}> $<TARGET_FILE_DIR:tlv_roundtrip_test>
        VERBATIM
    )
endforeach()
add_dependencies(tlv_roundtrip_test lzocompress lzodecompress)

add_test(NAME tlv_roundtrip COMMAND tlv_roundtrip_test)
