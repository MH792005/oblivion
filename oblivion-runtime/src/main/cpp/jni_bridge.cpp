#include <jni.h>
#include "oblivion_crypto.h"

namespace oblivion {
namespace daemon {
    bool start_security_daemon();
}
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_io_oblivion_runtime_OblivionCore_nativeStartSecurityDaemons(JNIEnv* env, jclass clazz) {
    (void)env;
    (void)clazz;
    return oblivion::daemon::start_security_daemon() ? JNI_TRUE : JNI_FALSE;
}

}
