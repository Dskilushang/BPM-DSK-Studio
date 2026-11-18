#include <jni.h>
#include <oboe/Oboe.h>
#include <vector>
#include <atomic>
#include <mutex>

struct Deck {
    std::vector<float> pcmData;
    double playhead = 0.0;
    double cuePoint = 0.0;
    std::atomic<bool> isPlaying{false};
    std::atomic<float> pitch{1.0f};
    std::mutex dataMutex;

    void loadTrack(const float* data, int length) {
        std::lock_guard<std::mutex> lock(dataMutex);
        isPlaying.store(false);
        pcmData.assign(data, data + length);
        playhead = 0.0;
        cuePoint = 0.0;
    }

    void triggerCue() {
        isPlaying.store(false);
        playhead = cuePoint;
    }

    void getNextStereoSample(float& left, float& right) {
        left = 0.0f;
        right = 0.0f;
        if (!isPlaying.load()) return;

        std::unique_lock<std::mutex> lock(dataMutex, std::try_to_lock);
        if (!lock.owns_lock() || pcmData.empty()) return;

        size_t totalFrames = pcmData.size() / 2;
        size_t index = static_cast<size_t>(playhead);

        if (index + 1 >= totalFrames) {
            isPlaying.store(false);
            playhead = 0.0;
            return;
        }

        double frac = playhead - index;
        size_t pos = index * 2;
        size_t nextPos = (index + 1) * 2;

        left = pcmData[pos] + frac * (pcmData[nextPos] - pcmData[pos]);
        right = pcmData[pos + 1] + frac * (pcmData[nextPos + 1] - pcmData[pos + 1]);

        playhead += pitch.load();
    }
};

class DJAudioEngine : public oboe::AudioStreamDataCallback {
public:
    Deck deckA;
    Deck deckB;
    std::atomic<float> crossfader{0.5f};

    void start() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output)
               ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
               ->setSharingMode(oboe::SharingMode::Exclusive)
               ->setFormat(oboe::AudioFormat::Float)
               ->setChannelCount(oboe::ChannelCount::Stereo)
               ->setSampleRate(44100)
               ->setDataCallback(this)
               ->openStream(stream);
        stream->requestStart();
    }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream*, void* audioData, int32_t numFrames) override {
        auto* out = static_cast<float*>(audioData);
        float cf = crossfader.load();
        float gainA = 1.0f - cf;
        float gainB = cf;

        for (int i = 0; i < numFrames; ++i) {
            float lA, rA, lB, rB;
            deckA.getNextStereoSample(lA, rA);
            deckB.getNextStereoSample(lB, rB);

            out[i * 2]     = (lA * gainA) + (lB * gainB);
            out[i * 2 + 1] = (rA * gainA) + (rB * gainB);
        }
        return oboe::DataCallbackResult::Continue;
    }

private:
    std::shared_ptr<oboe::AudioStream> stream;
};

static DJAudioEngine engine;

extern "C" {
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_startEngine(JNIEnv*, jobject) {
        engine.start();
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setCrossfader(JNIEnv*, jobject, jfloat val) {
        engine.crossfader.store(val);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_loadTrackA(JNIEnv* env, jobject, jfloatArray data) {
        jsize len = env->GetArrayLength(data);
        jfloat* ptr = env->GetFloatArrayElements(data, nullptr);
        engine.deckA.loadTrack(ptr, len);
        env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_toggleDeckA(JNIEnv*, jobject, jboolean play) {
        engine.deckA.isPlaying.store(play);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setPitchA(JNIEnv*, jobject, jfloat pitch) {
        engine.deckA.pitch.store(pitch);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_cueDeckA(JNIEnv*, jobject) {
        engine.deckA.triggerCue();
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_loadTrackB(JNIEnv* env, jobject, jfloatArray data) {
        jsize len = env->GetArrayLength(data);
        jfloat* ptr = env->GetFloatArrayElements(data, nullptr);
        engine.deckB.loadTrack(ptr, len);
        env->ReleaseFloatArrayElements(data, ptr, JNI_ABORT);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_toggleDeckB(JNIEnv*, jobject, jboolean play) {
        engine.deckB.isPlaying.store(play);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_setPitchB(JNIEnv*, jobject, jfloat pitch) {
        engine.deckB.pitch.store(pitch);
    }
    JNIEXPORT void JNICALL Java_com_example_bpmstudioclone_AudioBridge_cueDeckB(JNIEnv*, jobject) {
        engine.deckB.triggerCue();
    }
}
