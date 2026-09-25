/* Local/open speech engine for the grammar speaking tool.
   STT: Whisper Large-v3 Turbo via Transformers.js/ONNX.
   TTS: Kokoro-82M via Transformers.js/ONNX.
   No audio or text is sent to the AWS/server.js backend. */
(async () => {
  const $ = id => document.getElementById(id);
  const MODEL_ASR = "onnx-community/whisper-large-v3-turbo";
  const MODEL_TTS = "onnx-community/Kokoro-82M-v1.0-ONNX";
  const TRANSFORMERS_URL = "https://cdn.jsdelivr.net/npm/@huggingface/transformers@3.8.1/+esm";

  let transformers = null;
  let transcriber = null;
  let synthesizer = null;
  let mediaStream = null;
  let mediaRecorder = null;
  let audioChunks = [];
  let loadingASR = null;
  let loadingTTS = null;
  let speaking = false;
  let listenButtonRef = null;
  let silenceTimer = null;
  let analyser = null;
  let audioContext = null;
  let liveRecognition = null;

  function setStatus(message) {
    const el = $("grammarStatus");
    if (el) el.textContent = message;
  }

  async function getTransformers() {
    if (!transformers) transformers = await import(TRANSFORMERS_URL);
    return transformers;
  }

  async function loadASR() {
    if (transcriber) return transcriber;
    if (loadingASR) return loadingASR;
    loadingASR = (async () => {
      setStatus("Loading Whisper Large-v3 Turbo in your browser... The first load can take a while.");
      const { pipeline } = await getTransformers();
      const options = { dtype: "q4", device: "webgpu" };
      try {
        transcriber = await pipeline("automatic-speech-recognition", MODEL_ASR, options);
      } catch (webgpuError) {
        console.warn("Whisper WebGPU unavailable; trying browser WASM.", webgpuError);
        transcriber = await pipeline("automatic-speech-recognition", MODEL_ASR, { dtype: "q4", device: "wasm" });
      }
      return transcriber;
    })();
    try {
      return await loadingASR;
    } finally {
      loadingASR = null;
    }
  }

  async function loadTTS() {
    if (synthesizer) return synthesizer;
    if (loadingTTS) return loadingTTS;
    loadingTTS = (async () => {
      setStatus("Loading Kokoro voice in your browser...");
      const { pipeline } = await getTransformers();
      const options = { dtype: "q8", device: "webgpu" };
      try {
        synthesizer = await pipeline("text-to-speech", MODEL_TTS, options);
      } catch (webgpuError) {
        console.warn("Kokoro WebGPU unavailable; trying browser WASM.", webgpuError);
        synthesizer = await pipeline("text-to-speech", MODEL_TTS, { dtype: "q8", device: "wasm" });
      }
      return synthesizer;
    })();
    try {
      return await loadingTTS;
    } finally {
      loadingTTS = null;
    }
  }

  function pickMimeType() {
    const types = [
      "audio/webm;codecs=opus",
      "audio/webm",
      "audio/mp4",
      "audio/ogg;codecs=opus"
    ];
    return types.find(t => MediaRecorder.isTypeSupported(t)) || "";
  }

  async function decodeTo16kMono(blob) {
    const bytes = await blob.arrayBuffer();
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) throw new Error("This browser does not provide AudioContext.");
    const ctx = new AudioCtx();
    try {
      const buffer = await ctx.decodeAudioData(bytes.slice(0));
      const channels = buffer.numberOfChannels;
      const length = buffer.length;
      const mixed = new Float32Array(length);
      for (let c = 0; c < channels; c++) {
        const data = buffer.getChannelData(c);
        for (let i = 0; i < length; i++) mixed[i] += data[i] / channels;
      }
      if (buffer.sampleRate === 16000) return mixed;

      const targetLength = Math.max(1, Math.round(length * 16000 / buffer.sampleRate));
      const output = new Float32Array(targetLength);
      const ratio = buffer.sampleRate / 16000;
      for (let i = 0; i < targetLength; i++) {
        const pos = i * ratio;
        const left = Math.floor(pos);
        const right = Math.min(left + 1, length - 1);
        const frac = pos - left;
        output[i] = mixed[left] * (1 - frac) + mixed[right] * frac;
      }
      return output;
    } finally {
      try { await ctx.close(); } catch (_) {}
    }
  }

  async function transcribeBlob(blob) {
    const audio = await decodeTo16kMono(blob);
    const asr = await loadASR();
    setStatus("Whisper is transcribing your sentence locally...");
    const result = await asr(audio, {
      language: "english",
      task: "transcribe",
      chunk_length_s: 30,
      stride_length_s: 5
    });
    return (result && result.text ? result.text : "").trim();
  }

  async function recordSentence() {
    if (speaking) return;
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || !window.MediaRecorder) {
      throw new Error("This browser does not support local microphone recording.");
    }

    mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
    });

    audioChunks = [];
    const mimeType = pickMimeType();
    mediaRecorder = new MediaRecorder(mediaStream, mimeType ? { mimeType } : undefined);
    speaking = true;

    return new Promise((resolve, reject) => {
      mediaRecorder.ondataavailable = e => {
        if (e.data && e.data.size) audioChunks.push(e.data);
      };
      mediaRecorder.onerror = e => {
        speaking = false;
        reject(e.error || new Error("Microphone recording failed."));
      };
      mediaRecorder.onstop = async () => {
        speaking = false;
        try { if (liveRecognition) liveRecognition.stop(); } catch (_) {}
        liveRecognition = null;
        if (mediaStream) mediaStream.getTracks().forEach(t => t.stop());
        mediaStream = null;
        if (silenceTimer) { clearTimeout(silenceTimer); silenceTimer = null; }
        analyser = null;
        if (audioContext) { try { await audioContext.close(); } catch (_) {} audioContext = null; }
        const blob = new Blob(audioChunks, { type: mediaRecorder.mimeType || "audio/webm" });
        audioChunks = [];
        try {
          resolve(await transcribeBlob(blob));
        } catch (e) {
          reject(e);
        }
      };
      mediaRecorder.start();

      // Show live speech text while Whisper records the audio.
      // Whisper remains the final transcript used for correction.
      try {
        const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
        const live = SR ? new SR() : null;
        if (live) {
          liveRecognition = live;
          live.lang = "en-US";
          live.interimResults = true;
          live.continuous = true;
          live.onresult = e => {
            let shown = "";
            for (let i = 0; i < e.results.length; i++) {
              if (e.results[i]?.[0]) shown += e.results[i][0].transcript + " ";
            }
            shown = shown.trim();
            const liveBox = $("grammarLive");
            const liveText = $("grammarLiveText");
            const heardBox = $("grammarHeard");
            if (shown) {
              if (liveBox && liveText) {
                liveBox.className = "heard";
                liveText.textContent = shown;
              }
              // Also show the live transcript in the main correction result area.
              // This makes the spoken sentence visible while recording, just like
              // the Speaking Practice screen.
              if (heardBox) {
                heardBox.className = "heard";
                heardBox.innerHTML = "<b>You said:</b> " + shown.replace(/[&<>]/g, c => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;" }[c]));
              }
            }
          };
          live.onerror = () => {};
          live.onend = () => {
            if (speaking) {
              try { live.start(); } catch (_) {}
            }
          };
          live.start();
        }
      } catch (_) {}

      setStatus("🎤 Listening... speak your complete sentence. I will stop after a short pause.");
      try {
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const source = audioContext.createMediaStreamSource(mediaStream);
        analyser = audioContext.createAnalyser();
        analyser.fftSize = 2048;
        source.connect(analyser);
        const data = new Uint8Array(analyser.fftSize);
        let quietSince = null;
        const monitor = () => {
          if (!speaking || !analyser) return;
          analyser.getByteTimeDomainData(data);
          let sum = 0;
          for (let i = 0; i < data.length; i++) {
            const x = (data[i] - 128) / 128;
            sum += x * x;
          }
          const rms = Math.sqrt(sum / data.length);
          if (rms < 0.018) {
            if (!quietSince) quietSince = Date.now();
            if (Date.now() - quietSince > 1400 && Date.now() - (window.__speechStartedAt || Date.now()) > 900) {
              stopRecording();
              return;
            }
          } else {
            quietSince = null;
          }
          requestAnimationFrame(monitor);
        };
        window.__speechStartedAt = Date.now();
        requestAnimationFrame(monitor);
      } catch (_) {}
    });
  }

  function stopRecording() {
    if (mediaRecorder && mediaRecorder.state !== "inactive") mediaRecorder.stop();
  }

  async function playRawAudio(raw) {
    const blob = raw && typeof raw.toBlob === "function" ? raw.toBlob() : null;
    if (!blob) throw new Error("Kokoro did not return playable audio.");
    const url = URL.createObjectURL(blob);
    try {
      const audio = new Audio(url);
      audio.preload = "auto";
      await audio.play();
      await new Promise(resolve => {
        audio.onended = resolve;
        audio.onerror = resolve;
      });
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  async function speakCorrection(text) {
    const tts = await loadTTS();
    setStatus("Kokoro is generating the correction voice locally...");
    const output = await tts(text, { voice: "af_heart", speed: 1.0 });
    await playRawAudio(output);
    setStatus("Ready. You can Respeak or check another sentence.");
  }

  function setButtons(listening) {
    const speak = $("grammarSpeak");
    const respeak = $("grammarRespeak");
    const send = $("grammarSend");
    if (speak) {
      speak.disabled = false;
      speak.textContent = listening ? "⏹ Stop & transcribe" : "🎤 Speak a sentence";
    }
    if (respeak) respeak.disabled = false;
    if (send) send.disabled = listening || !(window.grammarPendingText || "").trim();
    if (listenButtonRef) listenButtonRef.disabled = listening;
  }

  function resetUI() {
    const live = $("grammarLive");
    const liveText = $("grammarLiveText");
    const heard = $("grammarHeard");
    const result = $("grammarResult");
    if (live) { live.className = "heard hidden"; liveText.textContent = ""; }
    if (heard) { heard.className = "heard hidden"; heard.innerHTML = ""; }
    if (result) { result.className = "feedback hidden"; result.innerHTML = ""; }
    const send = $("grammarSend");
    if (send) send.disabled = true;
    const listen = $("grammarListen");
    if (listen) listen.disabled = true;
  }

  async function handleWhisperSpeak() {
    try {
      if (speaking) {
        stopRecording();
        return;
      }
      resetUI();
      setButtons(true);
      const text = await recordSentence();
      setButtons(false);

      if (!text) {
        setStatus("Whisper could not hear a complete sentence. Tap Respeak and try again.");
        return;
      }

      window.grammarPendingText = text;
      window.grammarLastCorrection = text;
      const liveBox = $("grammarLive");
      const liveText = $("grammarLiveText");
      if (liveBox && liveText) {
        liveBox.className = "heard";
        liveText.textContent = text;
      }
      const heard = $("grammarHeard");
      if (heard) {
        heard.className = "heard";
        heard.innerHTML = "<b>You said:</b> " + text.replace(/[&<>]/g, c => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;" }[c]));
      }
      const send = $("grammarSend");
      if (send) send.disabled = text.trim().split(/\s+/).filter(Boolean).length < 2;
      setStatus("Whisper captured your sentence. Tap Check sentence.");
    } catch (e) {
      console.error("Whisper speech capture failed.", e);
      setButtons(false);
      setStatus("Microphone/Whisper error. Tap Respeak and try again.");
    }
  }

  function handleRespeak() {
    try { stopRecording(); } catch (_) {}
    window.grammarPendingText = "";
    resetUI();
    setButtons(false);
    setStatus("Ready. Tap Speak a sentence and say it again.");
  }

  async function handleHearCorrection() {
    const correction = window.kokoroCorrection || window.grammarPendingText;
    if (!correction) {
      setStatus("Check a sentence first.");
      return;
    }
    const button = $("grammarListen");
    if (button) button.disabled = true;
    try {
      await speakCorrection(correction);
    } catch (e) {
      console.error("Kokoro TTS failed.", e);
      setStatus("Kokoro voice could not start. Please try again.");
    } finally {
      if (button) button.disabled = false;
    }
  }

  // Override only the grammar speaking controls. Existing lesson/free-speech
  // code remains untouched.
  const speakButton = $("grammarSpeak");
  const respeakButton = $("grammarRespeak");
  const listenButton = $("grammarListen");
  const sendButton = $("grammarSend");
  if (speakButton) speakButton.onclick = handleWhisperSpeak;
  if (respeakButton) respeakButton.onclick = handleRespeak;
  if (listenButton) listenButton.onclick = handleHearCorrection;
  if (sendButton) sendButton.onclick = async () => {
    const text = (window.grammarPendingText || "").trim();
    if (speaking) { setStatus("Tap Stop & transcribe first."); return; }
    if (!text || !window.showGrammarToolResult) {
      setStatus("Speak a complete sentence first.");
      return;
    }
    try {
      sendButton.disabled = true;
      setStatus("Checking your sentence locally...");
      await window.showGrammarToolResult(text);
      const correction = $("grammarResult")?.querySelector(".correction")?.textContent || "";
      window.kokoroCorrection = correction.replace(/^Better sentence:\s*/i, "").trim() || text;
      if (listenButton) listenButton.disabled = false;
    } catch (e) {
      console.error("Grammar check bridge failed.", e);
      setStatus("Grammar check failed. Please try again.");
    } finally {
      sendButton.disabled = false;
    }
  };

  window.localSpeechAI = {
    transcribeBlob,
    speakCorrection,
    loadASR,
    loadTTS
  };

  const status = $("grammarStatus");
  if (status) status.textContent = "Ready. Whisper + Kokoro run locally in your browser.";
})();