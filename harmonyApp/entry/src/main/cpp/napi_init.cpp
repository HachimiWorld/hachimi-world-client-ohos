#include "libkn_api.h"
#include "napi/native_api.h"
#include "hilog/log.h"
#include <rawfile/raw_file_manager.h>
#include <dlfcn.h>

// 避免工程侧未定义 LOG_DOMAIN 时编译失败
#ifndef LOG_DOMAIN
#define LOG_DOMAIN 0x0000
#endif

static napi_value MainArkUIViewController(napi_env env, napi_callback_info info) {
    return reinterpret_cast<napi_value>(MainArkUIViewController(env));
}

extern "C" void* HachimiInitPlatform(void* env, void* info);
extern "C" void* HachimiDisposePlatform(void* env, void* info);

static napi_value InitPlatform(napi_env env, napi_callback_info info) {
    return reinterpret_cast<napi_value>(HachimiInitPlatform(env, info));
}

static napi_value DisposePlatform(napi_env env, napi_callback_info info) {
    return reinterpret_cast<napi_value>(HachimiDisposePlatform(env, info));
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports) {
    androidx_compose_ui_arkui_init(env, exports);
    napi_property_descriptor desc[] = {
        {"initPlatform", nullptr, InitPlatform, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"disposePlatform", nullptr, DisposePlatform, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"MainArkUIViewController", nullptr, MainArkUIViewController, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void*)0),
    .reserved = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void)
{
    napi_module_register(&demoModule);
}
