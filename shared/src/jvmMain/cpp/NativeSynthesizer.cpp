#include "include/nativesynthesizer.h"
#include <cmath>
#include <algorithm>
#include <random>
#include "bessel.h"

namespace synthesizer {

    NativeSynthesizer::NativeSynthesizer() {
        LOGD("Initializing JUCE AudioDeviceManager...");

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

        currentSampleRate = 48000.0;
        T = 1.0 / currentSampleRate;
        mModDecayFactorPerSample = std::pow(0.5, T / 0.5);

        std::random_device rd;
        std::mt19937 gen(rd());
        std::uniform_real_distribution<double> dist(0.0, 1.0);

        for (int v = 0; v < MAX_VOICES; ++v) {
            voices[v].amplitude.store(0.063f); // -24dB default
            voices[v].integral = dist(gen);
            voices[v].tau = 0.0;
            voices[v].integrand = 1.0;
            voices[v].playnote.store(false);
            voices[v].currentF0 = 0.0;
            voices[v].targetFrequency.store(0.0);
            voices[v].glideStep.store(0.0);
            voices[v].carrier.store(1);
            voices[v].modulator.store(1);
            voices[v].modulationIndex.store(1.0);
            voices[v].mModDecayScale = 1.0;
            voices[v].envelopeState = EnvelopeState::Idle;
            voices[v].envelopeValue = 0.0;
            voices[v].sampleCounter = 0;
            voices[v].noteOffCounter.store(UINT64_MAX);

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
        mModDecayFactorPerSample = std::pow(0.5, T / 0.5);

        for (int v = 0; v < MAX_VOICES; ++v) {
            voices[v].innerindex = 0.0;
            voices[v].integral = 0.0;
            voices[v].tau = 0.0;
            voices[v].integrand = 1.0;
            voices[v].envelopeState = EnvelopeState::Idle;
            voices[v].envelopeValue = 0.0;
            voices[v].sampleCounter = 0;
            voices[v].noteOffCounter.store(UINT64_MAX);
            voices[v].mModDecayScale = 1.0;
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
            if (!voice.playnote.load()) continue;

            for (int sample = 0; sample < numSamples; ++sample) {
                uint64_t counter = voice.sampleCounter;

                if (voice.envelopeState != EnvelopeState::Release &&
                    voice.envelopeState != EnvelopeState::Idle &&
                    counter >= voice.noteOffCounter.load()) {
                    voice.envelopeState = EnvelopeState::Release;
                }

                switch (voice.envelopeState) {
                    case EnvelopeState::Attack:
                        voice.envelopeValue += voice.attackStep.load();
                        if (voice.envelopeValue >= 1.0) {
                            voice.envelopeValue = 1.0;
                            voice.envelopeState = EnvelopeState::Decay;
                        }
                        break;

                    case EnvelopeState::Decay:
                        voice.envelopeValue -= voice.decayStep.load();
                        if (voice.envelopeValue <= voice.sustainLevel.load()) {
                            voice.envelopeValue = voice.sustainLevel.load();
                            voice.envelopeState = EnvelopeState::Sustain;
                        }
                        break;

                    case EnvelopeState::Sustain:
                        voice.envelopeValue = voice.sustainLevel.load();
                        break;

                    case EnvelopeState::Release:
                        voice.envelopeValue -= voice.releaseStep.load();
                        if (voice.envelopeValue <= 0.0) {
                            voice.envelopeValue = 0.0;
                            voice.envelopeState = EnvelopeState::Idle;
                            voice.playnote.store(false);
                            voice.currentF0 = 0.0;
                        }
                        break;

                    case EnvelopeState::Idle:
                        voice.envelopeValue = 0.0;
                        voice.playnote.store(false);
                        voice.currentF0 = 0.0;
                        break;
                }

                if (voice.playnote.load()) {
                    // Check pitch glide for smooth legato transitions
                    double step = voice.glideStep.load();
                    if (step != 0.0) {
                        double target = voice.targetFrequency.load();
                        double current = voice.currentF0;
                        current += step;

                        bool reached = (step > 0.0 && current >= target) || (step < 0.0 && current <= target);
                        if (reached) {
                            current = target;
                            voice.glideStep.store(0.0);
                        }
                        voice.currentF0 = current;
                    }

                    voice.sampleCounter++;

                    double currentF0 = voice.currentF0;
                    int cVal = voice.carrier.load();
                    int mVal = voice.modulator.load();
                    double baseModIdx = voice.modulationIndex.load();

                    double effectiveModIdx = baseModIdx * voice.mModDecayScale;
                    voice.mModDecayScale *= mModDecayFactorPerSample;

                    double fcVal = currentF0 * cVal;
                    double fmVal = currentF0 * mVal;
                    double amVal = effectiveModIdx * fmVal;

                    const double index = std::fmod(fmVal * (voice.tau + T), 1.0);
                    const double newintegrand = coSine(index);
                    voice.integral = std::fmod(1.0 + voice.integral + T * (fcVal + amVal * (voice.integrand + newintegrand) / 2.0), 1.0);

                    voice.integrand = newintegrand;
                    voice.tau += T;

                    double rawEnv = voice.envelopeValue;
                    double shapedEnv = rawEnv;
                    if (voice.envelopeState == EnvelopeState::Attack) {
                        shapedEnv = rawEnv * rawEnv;
                    }

                    float sampleVal = static_cast<float>((sine(voice.integral) - voice.m0) * shapedEnv * voice.amplitude.load());

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
            v.mModDecayScale = 1.0;
            v.playnote.store(true);
        }
    }

    void NativeSynthesizer::stop(int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            auto& v = voices[voiceIndex];
            v.noteOffCounter.store(v.sampleCounter);
            if (v.envelopeState != EnvelopeState::Idle) {
                v.envelopeState = EnvelopeState::Release;
            }
        }
    }

    bool NativeSynthesizer::isPlaying(int voiceIndex) const {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            return voices[voiceIndex].playnote.load();
        }
        return false;
    }

    void NativeSynthesizer::setFrequency(double frequencyInHz, int voiceIndex, double durationSeconds) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            auto& v = voices[voiceIndex];
            if (frequencyInHz > 1.0) {
                if (v.playnote.load() && v.currentF0 > 1.0 && v.envelopeState != EnvelopeState::Idle) {
                    // Legato transition: smooth 40ms pitch glide without clicking or phase reset
                    double current = v.currentF0;
                    v.targetFrequency.store(frequencyInHz);

                    constexpr double glideTimeSec = 0.040; // 40ms pitch glide
                    double totalGlideSamples = glideTimeSec * currentSampleRate;
                    if (totalGlideSamples > 1.0) {
                        v.glideStep.store((frequencyInHz - current) / totalGlideSamples);
                    } else {
                        v.currentF0 = frequencyInHz;
                        v.glideStep.store(0.0);
                    }

                    if (v.envelopeState == EnvelopeState::Release) {
                        v.envelopeState = EnvelopeState::Sustain;
                    }
                } else {
                    // New note attack
                    v.currentF0 = frequencyInHz;
                    v.targetFrequency.store(frequencyInHz);
                    v.glideStep.store(0.0);
                    v.sampleCounter = 0;
                    v.envelopeState = EnvelopeState::Attack;
                    v.envelopeValue = 0.0;
                    v.mModDecayScale = 1.0;
                }

                if (durationSeconds > 0.0) {
                    v.noteOffCounter.store(static_cast<uint64_t>(durationSeconds * currentSampleRate));
                } else {
                    v.noteOffCounter.store(UINT64_MAX);
                }
                v.playnote.store(true);
            } else {
                v.noteOffCounter.store(v.sampleCounter);
                if (v.envelopeState != EnvelopeState::Idle) {
                    v.envelopeState = EnvelopeState::Release;
                }
            }
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setCarrierRatio(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier.store(ratio);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setModulatorRatio(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulator.store(ratio);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setCMRatio(int c, int m, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier.store(c);
            voices[voiceIndex].modulator.store(m);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setModulationIndex(double index, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulationIndex.store(index);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setEnvelopeMode(int modeIndex, int voiceIndex) {
        if (voiceIndex < 0 || voiceIndex >= MAX_VOICES) return;
        auto& v = voices[voiceIndex];
        v.envelopeMode.store(modeIndex);

        switch (static_cast<EnvelopeMode>(modeIndex)) {
            case EnvelopeMode::ADSR:
                v.attackStep.store(1.0 / (0.025 * currentSampleRate));
                v.sustainLevel.store(0.4);
                v.decayStep.store((1.0 - 0.4) / (0.150 * currentSampleRate));
                v.releaseStep.store(0.4 / (0.080 * currentSampleRate));
                break;

            case EnvelopeMode::AD:
                v.attackStep.store(1.0 / (0.025 * currentSampleRate));
                v.sustainLevel.store(0.0);
                v.decayStep.store(1.0 / (0.250 * currentSampleRate));
                v.releaseStep.store(1.0 / (0.010 * currentSampleRate));
                break;

            case EnvelopeMode::AR:
                v.attackStep.store(1.0 / (0.030 * currentSampleRate));
                v.sustainLevel.store(1.0);
                v.decayStep.store(0.001);
                v.releaseStep.store(1.0 / (0.300 * currentSampleRate));
                break;

            case EnvelopeMode::GATE:
                v.attackStep.store(1.0 / (0.012 * currentSampleRate));
                v.sustainLevel.store(1.0);
                v.decayStep.store(0.001);
                v.releaseStep.store(1.0 / (0.010 * currentSampleRate));
                break;

            case EnvelopeMode::PERCUSSIVE:
                v.attackStep.store(1.0 / (0.002 * currentSampleRate));
                v.sustainLevel.store(0.0);
                v.decayStep.store(1.0 / (0.120 * currentSampleRate));
                v.releaseStep.store(1.0 / (0.010 * currentSampleRate));
                break;

            case EnvelopeMode::PLUCK:
                v.attackStep.store(1.0 / (0.001 * currentSampleRate));
                v.sustainLevel.store(0.0);
                v.decayStep.store(1.0 / (0.150 * currentSampleRate));
                v.releaseStep.store(1.0 / (0.010 * currentSampleRate));
                break;

            case EnvelopeMode::PAD:
                v.attackStep.store(1.0 / (0.400 * currentSampleRate));
                v.sustainLevel.store(0.8);
                v.decayStep.store((1.0 - 0.8) / (0.200 * currentSampleRate));
                v.releaseStep.store(0.8 / (0.600 * currentSampleRate));
                break;

            case EnvelopeMode::ORGAN:
                v.attackStep.store(1.0 / (0.008 * currentSampleRate));
                v.sustainLevel.store(1.0);
                v.decayStep.store(0.001);
                v.releaseStep.store(1.0 / (0.005 * currentSampleRate));
                break;

            case EnvelopeMode::FADE:
                v.attackStep.store(1.0 / (0.800 * currentSampleRate));
                v.sustainLevel.store(0.7);
                v.decayStep.store((1.0 - 0.7) / (0.200 * currentSampleRate));
                v.releaseStep.store(0.7 / (0.800 * currentSampleRate));
                break;

            case EnvelopeMode::DRUM:
                v.attackStep.store(1.0 / (0.0018 * currentSampleRate));
                v.sustainLevel.store(0.0);
                v.decayStep.store(1.0 / (0.100 * currentSampleRate));
                v.releaseStep.store(1.0 / (0.010 * currentSampleRate));
                break;
        }
    }

    void NativeSynthesizer::setAmplitude(float newAmplitude, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].amplitude.store(newAmplitude);
        }
    }

    void NativeSynthesizer::setModulationIndex2(double index, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulationIndex2.store(index);
        }
    }

    void NativeSynthesizer::setCarrierRatio2(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier2.store(ratio);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setModulatorRatio2(int ratio, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].modulator2.store(ratio);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::setCMRatio2(int c, int m, int voiceIndex) {
        if (voiceIndex >= 0 && voiceIndex < MAX_VOICES) {
            voices[voiceIndex].carrier2.store(c);
            voices[voiceIndex].modulator2.store(m);
            updateParms(voiceIndex);
        }
    }

    void NativeSynthesizer::updateParms(int voiceIndex) {
        if (voiceIndex < 0 || voiceIndex >= MAX_VOICES) return;
        auto& v = voices[voiceIndex];

        double currentF0 = v.currentF0;
        int cVal = v.carrier.load();
        int mVal = v.modulator.load();
        double modIdx = v.modulationIndex.load();

        v.fc = currentF0 * cVal;
        v.fm = currentF0 * mVal;
        v.am = modIdx * v.fm;
        v.fm2 = (v.fm * v.modulator2.load()) / v.carrier2.load();

        if (mVal > 0 && (cVal % mVal) == 0) {
            int k = -cVal / mVal;
            auto key = std::make_pair(k, modIdx);
            if (besselValue.find(key) != besselValue.end()) {
#if __cplusplus >= 201703L && defined(__cpp_lib_math_special_functions)
                try {
                    double bessel_j_value = std::cyl_bessel_j(k, modIdx);
                    v.m0 = static_cast<float>(bessel_j_value);
                } catch (...) {
                    v.m0 = 0.f;
                }
#else
                v.m0 = bessel_j_series(k, modIdx);
                besselValue[key] = v.m0;
#endif
            } else {
                v.m0 = bessel_j_series(k, modIdx);
                besselValue[key] = v.m0;
            }
        } else {
            v.m0 = 0.0;
        }
    }

}
