import { useState } from "react";
import "./App.css";
import type { ChatMessageType } from "./types/ChatMessageType";

const App = () => {
  const [chatId] = useState<string>(
    `session-${Math.random().toString(36).substring(7)}`
  );
  const [message, setMessage] = useState<string>("");
  const [chatLog, setChatLog] = useState<ChatMessageType[]>([]);
  const [file, setFile] = useState<File | null>(null);

  // 1. Handle PDF Upload
  const handleUpload = async () => {
    if (!file) {
      alert("Please select a file first.");
      return;
    }
    const formData = new FormData();
    formData.append("file", file);
    try {
      const res = await fetch(
        "http://localhost:8080/api/ingestion/upload-pdf",
        {
          method: "POST",
          body: formData,
        }
      );
      alert(await res.text());
    } catch (error) {
      console.error("Upload failed", error);
    }
  };

  // 2. Handle Streaming Chat
  const handleSend = () => {
    const userMsg = { role: "user", text: message };
    setChatLog((prev: any) => [...prev, userMsg, { role: "AI", text: "" }]);

    // Server-Sent Events (SSE) for streaming
    const url = `http://localhost:8080/api/chat/stream?chatId=${chatId}&message=${message}`;
    const eventSource = new EventSource(url);

    eventSource.onmessage = (event) => {
      const newChunk = event.data;
      if (!newChunk && newChunk !== "") return;

      setChatLog((prev) => {
        const newLog = [...prev];
        const lastMsgIndex = newLog.length - 1;
        const lastMsg = { ...newLog[lastMsgIndex] };

        // Initialize thinking state if it doesn't exist
        if (lastMsg.isThinking === undefined) lastMsg.isThinking = false;

        // Detect Thinking Tags
        if (newChunk.includes("<think>")) {
          lastMsg.isThinking = true;
          newLog[lastMsgIndex] = lastMsg;
          return newLog;
        }

        if (newChunk.includes("</think>")) {
          lastMsg.isThinking = false;
          newLog[lastMsgIndex] = lastMsg;
          return newLog;
        }

        // Only append to text if we are NOT thinking
        if (!lastMsg.isThinking) {
          const needsSpace =
            lastMsg.text.length > 0 &&
            /^[a-zA-Z0-9]/.test(newChunk) &&
            !/[\s\n]$/.test(lastMsg.text);

          lastMsg.text = lastMsg.text + (needsSpace ? " " : "") + newChunk;
        }

        newLog[lastMsgIndex] = lastMsg;
        return newLog;
      });
    };

    eventSource.onerror = () => eventSource.close();
    setMessage("");
  };

  return (
    <div
      style={{ padding: "20px", fontFamily: "sans-serif", maxWidth: "600px" }}
    >
      <h2>DocuChat 2026</h2>

      <div
        style={{
          marginBottom: "20px",
          border: "1px solid #ddd",
          padding: "10px",
        }}
      >
        <input type="file" onChange={(e: any) => setFile(e.target.files[0])} />
        <button onClick={handleUpload}>Upload PDF</button>
      </div>

      <div
        style={{
          height: "300px",
          overflowY: "auto",
          border: "1px solid #ccc",
          padding: "10px",
        }}
      >
        {chatLog.map((m, i) => (
          <div key={i} style={{ marginBottom: "10px" }}>
            <div style={{ display: "flex", gap: "10px" }}>
              <strong>{m.role}:</strong>

              {/* Show actual message text */}
              <div style={{ whiteSpace: "pre-wrap", textAlign: 'left' }}>{m.text}</div>
              {m.role === "AI" && m.isThinking && (
              <div
                style={{ fontStyle: "italic", color: "#888" }}
              >
                Thinking...
              </div>
            )}
            </div>
          </div>
        ))}
      </div>

      <div style={{ marginTop: "10px" }}>
        <input
          style={{ width: "80%" }}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask something..."
        />
        <button onClick={handleSend}>Send</button>
      </div>
    </div>
  );
};

export default App;
