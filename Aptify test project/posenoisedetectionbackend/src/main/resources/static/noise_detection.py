# import sounddevice as sd
# import numpy as np
# import sys
# import json
# import time
#
# SAMPLE_RATE = 44100
# BLOCK_SIZE = 1024
# NOISE_THRESHOLD = 0.01
# DURATION = 1  # Seconds to sample noise per iteration
#
# def calculate_noise_level():
#     rms_values = []
#
#     def audio_callback(indata, frames, time, status):
#         if status:
#             print(status)
#         rms = np.sqrt(np.mean(indata**2))
#         rms_values.append(rms)
#
#     with sd.InputStream(samplerate=SAMPLE_RATE, blocksize=BLOCK_SIZE, channels=1, callback=audio_callback):
#         sd.sleep(DURATION * 1000)  # Sleep for DURATION seconds
#
#     avg_rms = np.mean(rms_values) if rms_values else 0
#     return avg_rms
#
# def main():
#     output_path = "noise_result.json" if len(sys.argv) < 2 else sys.argv[1]
#     try:
#         while True:
#             rms = calculate_noise_level()
#             result = {
#                 "rms": float(rms),
#                 "is_noisy": bool(rms > NOISE_THRESHOLD)
#             }
#             try:
#                 with open(output_path, "w") as f:
#                     json.dump(result, f)
#             except Exception as e:
#                 print(f"Error writing to {output_path}: {e}")
#                 break
#             print(f"Noise RMS: {rms:.4f}, Is Noisy: {result['is_noisy']}", flush=True)
#             time.sleep(0.1)  # Short pause to avoid overloading
#     except KeyboardInterrupt:
#         print("Noise detection stopped.")
#     except Exception as e:
#         print(f"Error in noise detection: {e}")
#
# if __name__ == "__main__":
#     main()

import pyaudio
import numpy as np
import sys
import time

# Audio parameters
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 44100
CHUNK = 1024
NOISE_THRESHOLD = 0.001

# Initialize PyAudio
try:
    audio = pyaudio.PyAudio()
    stream = audio.open(format=FORMAT, channels=CHANNELS, rate=RATE, input=True, frames_per_buffer=CHUNK)
except Exception as e:
    print(f"Error initializing PyAudio: {e}", file=sys.stderr)
    sys.exit(1)

try:
    while True:
        try:
            # Read audio chunk
            data = stream.read(CHUNK, exception_on_overflow=False)
            # Convert to numpy array
            samples = np.frombuffer(data, dtype=np.int16)
            # Calculate RMS, handle potential invalid data
            squared_mean = np.mean(samples**2)
            if squared_mean < 0 or not np.isfinite(squared_mean):
                print("Invalid audio data, skipping...", file=sys.stderr)
                rms = 0.0
            else:
                rms = np.sqrt(squared_mean) / 32768.0  # Normalize to [0, 1]
            # Determine if noisy
            is_noisy = rms > NOISE_THRESHOLD
            # Output to stdout
#             print(f"Noise RMS: {rms:.6f}, Is Noisy: {is_noisy}", flush=True)
            time.sleep(0.1)
        except Exception as e:
            print(f"Error processing audio: {e}", file=sys.stderr)
            time.sleep(0.1)  # Avoid tight loop on error
except KeyboardInterrupt:
    pass
finally:
    stream.stop_stream()
    stream.close()
    audio.terminate()