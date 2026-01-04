export interface ChatMessageType {
    role: 'You' | 'AI';
    text: string;
    isThinking?: boolean;
  }