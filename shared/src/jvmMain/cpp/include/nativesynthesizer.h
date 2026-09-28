#pragma once

#include <iostream>
#include <juce_audio_basics/juce_audio_basics.h>
#include <juce_audio_devices/juce_audio_devices.h>
#include <juce_audio_utils/juce_audio_utils.h>
#include <array>
#include <map>
#include <atomic>
#include <cstdint>

#define LOGD(msg) std::cout << "[NativeSynthesizer] " << msg << std::endl

namespace synthesizer {

    constexpr int MAX_VOICES = 50;

    enum class EnvelopeState {
        Idle,
        Attack,
        Decay,
        Sustain,
        Release
    };

    enum class EnvelopeMode {
        ADSR = 0,
        AD = 1,
        AR = 2,
        GATE = 3,
        PERCUSSIVE = 4,
        PLUCK = 5,
        PAD = 6,
        ORGAN = 7,
        FADE = 8,
        DRUM = 9
    };

    struct Voice {
        std::atomic<bool> playnote{false};
        std::atomic<double> targetFrequency{440.0};
        double currentF0{440.0};
        std::atomic<double> glideStep{0.0};

        std::atomic<int> carrier{1};
        std::atomic<int> modulator{1};
        std::atomic<double> modulationIndex{1.0};
        double mModDecayScale{1.0};

        std::atomic<int> carrier2{1};
        std::atomic<int> modulator2{1};
        std::atomic<double> modulationIndex2{0.0};

        double fc{440.0};
        double fm{440.0};
        double am{1.0};
        double fm2{440.0};

        double integral{0.0};
        double tau{0.0};
        double integrand{1.0};
        double innerindex{0.0};
        double m0{0.0};

        std::atomic<float> amplitude{0.063f};

        // Envelope state
        EnvelopeState envelopeState{EnvelopeState::Idle};
        double envelopeValue{0.0};
        uint64_t sampleCounter{0};
        std::atomic<uint64_t> noteOffCounter{UINT64_MAX};

        std::atomic<int> envelopeMode{0}; // ADSR default
        std::atomic<double> attackStep{0.001};
        std::atomic<double> decayStep{0.001};
        std::atomic<double> sustainLevel{0.7};
        std::atomic<double> releaseStep{0.001};
    };

    class NativeSynthesizer : public juce::AudioSource {
    public:
        NativeSynthesizer();
        ~NativeSynthesizer() override;

        void play(int voiceIndex = 0);
        void stop(int voiceIndex = 0);
        bool isPlaying(int voiceIndex = 0) const;
        void setFrequency(double frequencyInHz, int voiceIndex = 0, double durationSeconds = 0.0);
        void setCarrierRatio(int ratio, int voiceIndex = 0);
        void setModulatorRatio(int ratio, int voiceIndex = 0);
        void setCMRatio(int c, int m, int voiceIndex = 0);
        void setModulationIndex(double index, int voiceIndex = 0);
        void setEnvelopeMode(int modeIndex, int voiceIndex = 0);
        void setAmplitude(float newAmplitude, int voiceIndex = 0);

        // 2nd order FM methods
        void setModulationIndex2(double index, int voiceIndex = 0);
        void setCarrierRatio2(int ratio, int voiceIndex = 0);
        void setModulatorRatio2(int ratio, int voiceIndex = 0);
        void setCMRatio2(int c, int m, int voiceIndex = 0);

        // juce::AudioSource overrides
        void prepareToPlay(int samplesPerBlockExpected, double sampleRate) override;
        void getNextAudioBlock(const juce::AudioSourceChannelInfo& bufferToFill) override;
        void releaseResources() override;
        void updateParms(int voiceIndex = 0);

    private:
        juce::ScopedJuceInitialiser_GUI juceInitialiser;
        juce::AudioDeviceManager deviceManager;
        juce::AudioSourcePlayer sourcePlayer;

        double currentSampleRate{48000.0};
        double T{1.0 / 48000.0};
        double mModDecayFactorPerSample{1.0};

        std::array<Voice, MAX_VOICES> voices;

        std::array<float, 512> sineArray;
        std::array<float, 512> coSineArray;

        double PI = juce::MathConstants<double>::pi;
        double c = 512.0;

        float sine(double t) const {
            double s = t * c;
            int i = static_cast<int>(s);
            double fraction = s - i;
            return (1.0 - fraction) * sineArray[i % 512] + fraction * sineArray[(i + 1) % 512];
        }

        float coSine(double t) const {
            double s = t * c;
            int i = static_cast<int>(s);
            double fraction = s - i;
            return (1.0 - fraction) * coSineArray[i % 512] + fraction * coSineArray[(i + 1) % 512];
        }

        std::map<std::pair<int, double>, float> besselValue;
    };

}
