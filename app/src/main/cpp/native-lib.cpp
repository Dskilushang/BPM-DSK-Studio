#include <jni.h>
#include <oboe/Oboe.h>
#include <vector>
#include <atomic>
#include <mutex>
#include <cmath>
#include <algorithm>

template <typename T>
T clampVal(T val, T minVal, T maxVal) {
    return std::max(minVal, std::min(val, maxVal));
}

struct ThreeBandEQ {
    float lowGain = 1.0f;
    float midGain = 1.0f;
    float highGain = 1.0f;
    float lowStateL = 0.0f, lowStateR = 0.0f;
    float midStateL = 0.0f, midStateR = 0.0f;

    void process(float& left, float& right) {
        const float lowAlpha = 0.04f;
        const float highAlpha = 0.35f;

        lowStateL += lowAlpha * (left - lowStateL);
        lowStateR += lowAlpha * (right - lowStateR);

        midStateL += highAlpha * (left - midStateL);
        midStateR += highAlpha * (right - midStateR);

        float lowL = lowStateL;
        float lowR = lowStateR;
        float midL = midStateL - lowStateL;
        float midR = midStateR - lowStateR;
        float highL = left - midStateL;
        float highR = right - midStateR;

        left = (lowL * lowGain) + (midL * midGain) + (highL * highGain);
        right = (lowR * lowGain) + (midR * midGain) + (highR * highGain);
    }
};

struct Deck {
    std::vector<float> pcmData;
    double playhead = 0.0;
    double cuePoint = 0.0;
    double loopIn = -1.0;
    double loopOut = -1.0;
    std::atomic<bool> loopActive{false};
    std::atomic<bool> isPlaying{false};
    std::atomic<float> pitch{1.0f};
    std::atomic<float> pitchBend{0.0f};
    std::atomic<bool> pitchEnabled{true};
    std::atomic<float> volume{1.0f};
    std::atomic<float> currentLevel{0.0f};
    ThreeBandEQ eq;
    std::mutex dataMutex;

    void loadTrack(const float* data, int length) {
        std::lock_guard<std::mutex> lock(dataMutex);
        isPlaying.store(false);
        loopActive.store(false);
        loopIn = -1.0;
        loopOut = -1.0;
        pcmData.assign(data, data + length);
        playhead = 0.0;
        cuePoint = 0.0;
    }

    void triggerCue() {
        isPlaying.store(false);
        playhead = cuePoint;
    }

    void triggerCup() {
        playhead = cuePoint;
        isPlaying.store(true);
    }

    void seekNormalized(float normPos) {
        std::lock_guard<std::mutex> lock(dataMutex);
        if (pcmData.size() < 4) return;
        double maxFrame = static_cast<double>((pcmData.size() / 2) - 2);
        playhead = clampVal(static_cast<double>(normPos) * maxFrame, 0.0, maxFrame);
        if (!isPlaying.load()) {
            cuePoint = playhead;
        }
    }

    float getNormalizedPosition() {
        if (pcmData.empty()) return 0.0f;
        size_t totalFrames = pcmData.size() / 2;
        return static_cast<float>(playhead / static_cast<double>(totalFrames));
    }

    double getElapsedSeconds() { return playhead / 44100.0; }
    double getTotalSeconds() { return (pcmData.size() / 2) / 44100.0; }

    void setLoopIn() {
        loopIn = playhead;
        loopActive.store(false);
    }

    void setLoopOut() {
        if (loopIn >= 0.0 && playhead > loopIn) {
            loopOut = playhead;
            loopActive.store(true);
            playhead = loopIn;
        }
    }

    void toggleReloop() {
        if (loopIn >= 0.0 && loopOut > loopIn) {
            bool nextState = !loopActive.load();
            loopActive.store(nextState);
            if (nextState) playhead = loopIn;
        }
    }

    void getNextStereoSample(float& left, float& right) {
        left = 0.0f;
        right = 0.0f;
        if (!isPlaying.load()) {
            currentLevel.store(0.0f);
            return;
        }

        std::unique_lock<std::mutex> lock(dataMutex, std::try_to_lock);
        if (!lock.owns_lock() || pcmData.empty()) return;

        if (loopActive.load() && loopIn >= 0.0 && loopOut > loopIn && playhead >= loopOut) {
            playhead = loopIn;
        }

        size_t totalFrames = pcmData.size() / 2;
        size_t index = static_cast<size_t>(playhead);

        if (index + 1 >= totalFrames) {
            isPlaying.store(false);
            playhead = 0.0;
            currentLevel.store(0.0f);
            return;
        }

        double frac = playhead - index;
        size_t pos = index * 2;
        size_t nextPos = (index + 1) * 2;

        left = pcmData[pos] + frac * (pcmData[nextPos] - pcmData[pos]);
        right = pcmData[pos + 1] + frac * (pcmData[nextPos + 1] - pcmData[pos + 1]);

        eq.process(left, right);

        float vol = volume.load();
        left *= vol;
        right *= vol;

        currentLevel.store(std::max(std::abs(left), std::abs(right)));

        float effectivePitch = (pitchEnabled.load() ? pitch.load() : 1.0f) + pitchBend.load();
        playhead += std::max(0.1f, effectivePitch);
    }
};

class DJAudioEngine : public oboe::AudioStreamDataCallback {
public:
    Deck deckA;
    Deck deckB;
    std::atomic<float> crossfader{0.0f};
    std::atomic<float> masterVolume{1.0f};
    std::atomic<bool> autoFadeActive{false};
    std::atomic<float> autoFadeDirection{1.0f};
    std::atomic<float> autoFadeDurationSec{5.0f};

    void start() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
               ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
               ->setSharingMode(oboe::SharingMode::Shared)
               ->setFormat(oboe::AudioFormat::Float)
               ->setChannelCount(oboe::ChannelCount::Stereo)
               ->setSampleRate(44100)
               ->setDataCallback(this)
               ->openStream(stream);
        if (stream) {
            stream->requestStart();
        }
    }

    void triggerAutoFade() {
        float currentCf = crossfader.load();
        if (currentCf < 0.5f) {
            autoFadeDirection.store(1.0f);
            if (!deckB.isPlaying.load() && !deckB.pcmData.empty()) {
                deckB.isPlaying.store(true);
            }
        } else {
            autoFadeDirection.store(-1.0f);
            if (!deckA.isPlaying.load() && !deckA.pcmData.empty()) {
                deckA.isPlaying.store(true);
            }
        }
        autoFadeActive.store(true);
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void* audioData, int32_t numFrames) override {
        auto* out = static_cast<float*>(audioData);
        float master = masterVolume.load();

        for (int i = 0; i < numFrames; ++i) {
            if (autoFadeActive.load()) {
                float step = 1.0f / (44100.0f * std::max(0.5f, autoFadeDurationSec.load()));
                float nextCf = crossfader.load() + (autoFadeDirection.load() * step);
                if (nextCf >= 1.0f) {
                    nextCf = 1.0f;
                    autoFadeActive.store(false);
                    deckA.isPlaying.store(false);
                } else if (nextCf <= 0.0f) {
                    nextCf = 0.0f;
                    autoFadeActive.store(false);
                    deckB.isPlaying.store(false);
                }
                crossfader.store(nextCf);
            }

            float cf = crossfader.load();
            float gainA = std::cos(cf * 1.5707963f);
            float gainB = std::sin(cf * 1.5707963f);

            float lA, rA, lB, rB;
            deckA.getNextStereoSample(lA, rA);
            deckB.getNextStereoSample(lB, rB);

            out[i * 2]     = clampVal(((lA * gainA) + (lB * gainB)) * master, -1.0f, 1.0f);
            out[i * 2 + 1] = clampVal(((rA * gainA) + (rB * gainB)) * master, -1.0f, 1.0f);
        }
        return oboe::DataCallbackResult::Continue;
    }

private:
    std::shared_ptr<oboe::AudioStream> stream;
};

static DJAudioEngine engine;

extern "C" {
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_startEngine(JNIEnv*, jobject) { engine.start(); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setCrossfader(JNIEnv*, jobject, jfloat val) {
        engine.autoFadeActive.store(false);
        engine.crossfader.store(val);
    }
    JNIEXPORT jfloat JNICALL Java_com_example_bpmstudioclone_AudioBridge_getCrossfader(JNIEnv*, jobject) { return engine.crossfader.load(); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_triggerAutoFade(JNIEnv*, jobject) { engine.triggerAutoFade(); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setAutoFadeTime(JNIEnv*, jobject, jfloat sec) { engine.autoFadeDurationSec.store(sec); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setMasterVolume(JNIEnv*, jobject, jfloat vol) { engine.masterVolume.store(vol); }

    // DECK A
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_loadTrackA(JNIEnv* env, jobject, jfloatArray data) {
        jsize len = env->GetArrayLength(data);
        jfloat* ptr = env->GetFloatArrayElements(data, nullptr);
        engine.deckA.loadTrack(ptr, len);
        env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_toggleDeckA(JNIEnv*, jobject, jboolean play) { engine.deckA.isPlaying.store(play); }
    JNIEXPORT jboolean JNICALL Java_com_example_bpmstudioclone_AudioBridge_isPlayingA(JNIEnv*, jobject) { return engine.deckA.isPlaying.load(); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setPitchA(JNIEnv*, jobject, jfloat pitch) { engine.deckA.pitch.store(pitch); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setPitchBendA(JNIEnv*, jobject, jfloat bend) { engine.deckA.pitchBend.store(bend); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setPitchEnabledA(JNIEnv*, jobject, jboolean en) { engine.deckA.pitchEnabled.store(en); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_cueDeckA(JNIEnv*, jobject) { engine.deckA.triggerCue(); }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_cupDeckA(JNIEnv*, jobject) { engine.deckA.triggerCup(); }
