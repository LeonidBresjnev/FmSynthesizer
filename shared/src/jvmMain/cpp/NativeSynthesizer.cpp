#include "include/nativesynthesizer.h"
#include <cmath>
#include "bessel.h"

namespace synthesizer {

    NativeSynthesizer::NativeSynthesizer() {
        LOGD("Initializing JUCE AudioDeviceManager...");

        // Initialise audio device manager with 0 inputs and 2 outputs (stereo)
        juce::AudioDeviceManager::AudioDeviceSetup setup;
        deviceManager.getAudioDeviceSetup(setup);
        setup.sampleRate = 48000.0;
        setup.bufferSize = 512;
        deviceManager.setAudioDeviceSetup(setup, true);

        deviceManager.initialiseWithDefaultDevices(0, 2);

        sourcePlayer.setSource(this);
        deviceManager.addAudioCallback(&sourcePlayer);

        LOGD("NativeSynthesizer initialized.");
        for (int i = 0; i < 512; ++i) {
            sineArray[i] = std::sin(static_cast<double>(i) * 2.0 * PI / 512);
            coSineArray[i] = std::cos(static_cast<double>(i) * 2.0 * PI / 512);
        }

        for (int v = 0; v < MAX_VOICES; ++v) {
            voices[v].amplitude = 0.063f; // -24dB default
            setEnvelopeMode(0, v); // ADSR default
            updateParms(v);
        }
    }

    NativeSynthesizer::~NativeSynthesizer() {
        LOGD("Destroying NativeSynthesizer...");
        deviceManager.removeAudioCallback(&sourcePlayer);
        sourcePlayer.setSource(nullptr);
    }

    void NativeSynthesizer::prepareToPlay(int samplesPerBlockExpected, double sampleRate) {
        LOGD("prepareToPlay called: sampleRate=" << sampleRate << ", blockSize=" << samplesPerBlockExpected);
        currentSampleRate = (sampleRate > 0) ? sampleRate : 48000.0;
        T = 1.0 / currentSampleRate;
        for (int v = 0; v < MAX_VOICES; ++v) {
            voices[v].innerindex = 0.0;
            voices[v].integral = 0.0;
            voices[v].tau = 0.0;
            voices[v].integrand = 1.0;
            voices[v].envelopeState = EnvelopeState::Idle;
            voices[v].envelopeValue = 0.0;
            voices[v].sampleCounter = 0;
            voices[v].noteOffCounter = UINT64_MAX;
        }
    }

    void NativeSynthesizer::releaseResources() {
        LOGD("releaseResources called");
    }

    void NativeSynthesizer::getNextAudioBlock(const juce::AudioSourceChannelInfo& bufferToFill) {
        auto* buffer = bufferToFill.buffer;
        int numChannels = buffer->getNumChannels();
        int numSamples = bufferToFill.numSamples;
        int startSample = bufferToFill.startSample;

        bufferToFill.clearActiveBufferRegion();

        for (int v = 0; v < MAX_VOICES; ++v) {
            auto& voice = voices[v];
            if (!voice.playnote) continue;

            for (int sample = 0; sample < numSamples; ++sample) {
                uint64_t counter = voice.sampleCounter;

                if (voice.envelopeState != EnvelopeState::Release &&
                    voice.envelopeState != EnvelopeState::Idle &&
                    counter >= voice.noteOffCounter) {
                    voice.envelopeState = EnvelopeState::Release;
                }

                switch (voice.envelopeState) {
                    case EnvelopeState::Attack:
                        voice.envelopeValue += voice.attackStep;
                        if (voice.envelopeValue >= 1.0) {
                            voice.envelopeValue = 1.0;
                            voice.envelopeState = EnvelopeState::Decay;
                        }
                        break;

                    case EnvelopeState::Decay:
                        voice.envelopeValue -= voice.decayStep;
                        if (voice.envelopeValue <= voice.sustainLevel) {
                            voice.envelopeValue = voice.sustainLevel;
                            voice.envelopeState = EnvelopeState::Sustain;
                        }
                        break;

                    case EnvelopeState::Sustain:
                        voice.envelopeValue = voice.sustainLevel;
                        break;

                    case EnvelopeState::Release:
                        voice.envelopeValue -= voice.releaseStep;
                        if (voice.envelopeValue <= 0.0) {
                            voice.envelopeValue = 0.0;
                            voice.envelopeState = EnvelopeState::Idle;
                            voice.playnote = false;
                        }
                        break;

                    case EnvelopeState::Idle:
                        voice.envelopeValue = 0.0;
                        voice.playnote = false;
                        break;
                }

                if (voice.playnote) {
                    voice.sampleCounter++;

                    const double index = std::fmod(voice.fm * (voice.tau + T), 1.0);
                    const double newintegrand = coSine(index);
                    voice.integral = std::fmod(1.0 + voice.integral + T * (voice.fc + voice.am * (voice.integrand + newintegrand) / 2.0), 1.0);

                    voice.integrand = newintegrand;
                    voice.tau += T;

                    double rawEnv = voice.envelopeValue;
                    double shapedEnv = rawEnv;
                    if (voice.envelopeState == EnvelopeState::Attack) {
                        shapedEnv = rawEnv * rawEnv;
                    }

                    float sampleVal = static_cast<float>((sine(voice.integral) - voice.m0) * shapedEnv * voice.amplitude);

                    for (int channel = 0; channel < numChannels; ++channel) {
                        buffer->addSample(channel, startSample + sample, sampleVal);
                    }
                }
            }
        }
    }

    void NativeSynthesizer::play(int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            auto& v = voices[voiceIndex];
            v.sampleCounter = 0;
            v.envelopeState = EnvelopeState::Attack;
            v.envelopeValue = 0.0;
            v.playnote = true;
        }
    }

    void NativeSynthesizer::stop(int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            auto& v = voices[voiceIndex];
            v.noteOffCounter = v.sampleCounter;
            if (v.envelopeState != EnvelopeState::Idle) {
                v.envelopeState = EnvelopeState::Release;
            }
        }
    }

    bool NativeSynthesizer::isPlaying(int voiceIndex) const {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            return voices[voiceIndex].playnote;
        }
        return false;
    }

    void NativeSynthesizer::setFrequency(double frequencyInHz, int voiceIndex, double durationSeconds) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            auto& v = voices[voiceIndex];
            if (frequencyInHz > 1.0) {
                v.targetFrequency = frequencyInHz;
                v.sampleCounter = 0;
                v.envelopeState = EnvelopeState::Attack;
                v.envelopeValue = 0.0;

                if (durationSeconds > 0.0) {
                    v.noteOffCounter = static_cast<uint64_t>(durationSeconds * currentSampleRate);
                } else {
                    v.noteOffCounter = UINT64_MAX;
                }
                v.playnote = true;
            } else {
                v.noteOffCounter = v.sampleCounter;
                if (v.envelopeState != EnvelopeState::Idle) {
                    v.envelopeState = EnvelopeState::Release;
                }
            }
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setCarrierRatio(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier = ratio;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setModulatorRatio(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulator = ratio;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setCMRatio(int c, int m, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier = c;
            voices[voiceIndex].modulator = m;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setModulationIndex(double index, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulationIndex = index;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setEnvelopeMode(int modeIndex, int voiceIndex) {
        if (voiceIndex < 0 || voiceIndex >= MAX_VOICES) return;
        auto& v = voices[voiceIndex];
        v.envelopeMode = modeIndex;

        switch (static_cast<EnvelopeMode>(modeIndex)) {
            case EnvelopeMode::ADSR:
                v.attackStep = 1.0 / (0.025 * currentSampleRate);
                v.sustainLevel = 0.4;
                v.decayStep = (1.0 - 0.4) / (0.150 * currentSampleRate);
                v.releaseStep = 0.4 / (0.080 * currentSampleRate);
                break;

            case EnvelopeMode::AD:
                v.attackStep = 1.0 / (0.025 * currentSampleRate);
                v.sustainLevel = 0.0;
                v.decayStep = 1.0 / (0.250 * currentSampleRate);
                v.releaseStep = 1.0 / (0.010 * currentSampleRate);
                break;

            case EnvelopeMode::AR:
                v.attackStep = 1.0 / (0.030 * currentSampleRate);
                v.sustainLevel = 1.0;
                v.decayStep = 0.001;
                v.releaseStep = 1.0 / (0.300 * currentSampleRate);
                break;

            case EnvelopeMode::GATE:
                v.attackStep = 1.0 / (0.012 * currentSampleRate);
                v.sustainLevel = 1.0;
                v.decayStep = 0.001;
                v.releaseStep = 1.0 / (0.010 * currentSampleRate);
                break;

            case EnvelopeMode::PERCUSSIVE:
                v.attackStep = 1.0 / (0.002 * currentSampleRate);
                v.sustainLevel = 0.0;
                v.decayStep = 1.0 / (0.120 * currentSampleRate);
                v.releaseStep = 1.0 / (0.010 * currentSampleRate);
                break;

            case EnvelopeMode::PLUCK:
                v.attackStep = 1.0 / (0.001 * currentSampleRate);
                v.sustainLevel = 0.0;
                v.decayStep = 1.0 / (0.150 * currentSampleRate);
                v.releaseStep = 1.0 / (0.010 * currentSampleRate);
                break;

            case EnvelopeMode::PAD:
                v.attackStep = 1.0 / (0.400 * currentSampleRate);
                v.sustainLevel = 0.8;
                v.decayStep = (1.0 - 0.8) / (0.200 * currentSampleRate);
                v.releaseStep = 0.8 / (0.600 * currentSampleRate);
                break;

            case EnvelopeMode::ORGAN:
                v.attackStep = 1.0 / (0.008 * currentSampleRate);
                v.sustainLevel = 1.0;
                v.decayStep = 0.001;
                v.releaseStep = 1.0 / (0.005 * currentSampleRate);
                break;

            case EnvelopeMode::FADE:
                v.attackStep = 1.0 / (0.800 * currentSampleRate);
                v.sustainLevel = 0.7;
                v.decayStep = (1.0 - 0.7) / (0.200 * currentSampleRate);
                v.releaseStep = 0.7 / (0.800 * currentSampleRate);
                break;

            case EnvelopeMode::DRUM:
                v.attackStep = 1.0 / (0.0018 * currentSampleRate);
                v.sustainLevel = 0.0;
                v.decayStep = 1.0 / (0.100 * currentSampleRate);
                v.releaseStep = 1.0 / (0.010 * currentSampleRate);
                break;
        }
    }

    void NativeSynthesizer::setAmplitude(float newAmplitude, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].amplitude = newAmplitude;
        }
    }

    void NativeSynthesizer::setModulationIndex2(double index, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulationIndex2 = index;
        }
    }

    void NativeSynthesizer::setCarrierRatio2(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier2 = ratio;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setModulatorRatio2(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulator2 = ratio;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setCMRatio2(int c, int m, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier2 = c;
            voices[voiceIndex].modulator2 = m;
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::updateParms(int voiceIndex) {
        if (voiceIndex < 0 || voiceIndex >= MAX_VOICES) return;
        auto& v = voices[voiceIndex];

        v.fc = v.targetFrequency * v.carrier;
        v.fm = v.targetFrequency * v.modulator;
        v.am = v.modulationIndex * v.fm;
        v.fm2 = (v.fm * v.modulator2) / v.carrier2;

        if (v.modulator > 0 && (v.carrier % v.modulator) == 0) {
            int k = -v.carrier / v.modulator;
            if (besselValue.find(std::make_pair(k, v.modulationIndex)) != besselValue.end()) {
#if __cplusplus >= 201703L && defined(__cpp_lib_math_special_functions)
                try {
                    double bessel_j_value = std::cyl_bessel_j(k, static_cast<double>(v.modulationIndex));
                    v.m0 = static_cast<float>(bessel_j_value);
                } catch (...) {
                    v.m0 = 0.f;
                }
#else
                v.m0 = bessel_j_series(k, v.modulationIndex);
                besselValue[std::make_pair(k, v.modulationIndex)] = v.m0;
#endif
            } else {
                v.m0 = bessel_j_series(k, v.modulationIndex);
                besselValue[std::make_pair(k, v.modulationIndex)] = v.m0;
            }
        } else {
            v.m0 = 0.0;
        }
    }

}
