#include <jni.h>

JNIEXPORT jstring JNICALL
JAVA_com_example_senseit_getAPI(JNIEnv *env, jclass type){
    return (*env) -> NewStringUTF(env, "Hello there");
}