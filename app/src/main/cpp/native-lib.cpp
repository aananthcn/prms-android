#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nonprofit_aananth_prms_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "மன்னுயிர் ஓம்பி அருளாள்வார்க்கு இல்லென்ப\n தன்னுயிர் அஞ்சும் வினை.";
    return env->NewStringUTF(hello.c_str());
}
