#include <jni.h>
#include <iostream>
#include "nativesynthesizer.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_create(JNIEnv *env, jobject thiz) {
    auto *synth = new synthesizer::NativeSynthesizer();
    return reinterpret_cast<jlong>(synth);
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_delete(JNIEnv *env, jobject thiz, jlong synthesizer_handle) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        delete synth;
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_stop(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->stop(static_cast<int>(instrument_index));
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_play(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->play(static_cast<int>(instrument_index));
    }
}

JNIEXPORT jboolean JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_isPlaying(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        return synth->isPlaying(static_cast<int>(instrument_index)) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_setFrequency(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jfloat frequency_in_hz, jint instrument_index, jfloat duration_seconds) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->setFrequency(static_cast<double>(frequency_in_hz), static_cast<int>(instrument_index), static_cast<double>(duration_seconds));
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_setModulationIndex(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jfloat index, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->setModulationIndex(static_cast<double>(index), static_cast<int>(instrument_index));
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_setCMRatio(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jint c, jint m, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->setCMRatio(static_cast<int>(c), static_cast<int>(m), static_cast<int>(instrument_index));
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_setEnvelopeMode(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jint mode_index, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->setEnvelopeMode(static_cast<int>(mode_index), static_cast<int>(instrument_index));
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_setModulationIndex2(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jfloat index, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->setModulationIndex2(static_cast<double>(index), static_cast<int>(instrument_index));
    }
}

JNIEXPORT void JNICALL
Java_com_sputnik_fmsynthesizer_model_LoggingFmSynthesizer_setCMRatio2(JNIEnv *env, jobject thiz, jlong synthesizer_handle, jint c, jint m, jint instrument_index) {
    if (synthesizer_handle != 0) {
        auto *synth = reinterpret_cast<synthesizer::NativeSynthesizer *>(synthesizer_handle);
        synth->setCMRatio2(static_cast<int>(c), static_cast<int>(m), static_cast<int>(instrument_index));
    }
}

}
