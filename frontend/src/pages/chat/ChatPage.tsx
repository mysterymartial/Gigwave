import { useState, useEffect, useRef, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useChatThread, useChatMessages, useSendMessage, useOpenThread } from '../../hooks/useChat';
import { useAuth } from '../../hooks/useAuth';
import { useUploadChatMedia } from '../../hooks/useFiles';
import { format } from 'date-fns';

export default function ChatPage() {
  const { bookingId } = useParams<{ bookingId: string }>();
  if (!bookingId) return <div>Booking ID required</div>;
  const { user } = useAuth();
  const { data: thread, isLoading: threadLoading } = useChatThread(bookingId);
  const { data: messages, isLoading: messagesLoading } = useChatMessages(thread?.id || '');
  const sendMessage = useSendMessage();
  const openThread = useOpenThread();
  const uploadMedia = useUploadChatMedia();
  const [text, setText] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!thread && bookingId) {
      openThread.mutate(bookingId);
    }
  }, [thread, bookingId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (e: FormEvent) => {
    e.preventDefault();
    if (!thread?.id || (!text && !file)) return;

    if (file) {
      const uploadResult = await uploadMedia.mutateAsync({ bookingId, file });
      await sendMessage.mutateAsync({
        threadId: thread.id,
        data: {
          messageType: file.type.startsWith('image/') ? 'IMAGE' : 'VIDEO',
          mediaUrl: uploadResult.url,
        },
      });
      setFile(null);
    } else if (text) {
      await sendMessage.mutateAsync({
        threadId: thread.id,
        data: {
          messageType: 'TEXT',
          text,
        },
      });
      setText('');
    }
  };

  if (threadLoading || messagesLoading) {
    return <div className="text-center py-8">Loading...</div>;
  }

  return (
    <div className="max-w-4xl mx-auto h-[calc(100vh-200px)] flex flex-col">
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Chat</h1>
      <div className="flex-1 overflow-y-auto bg-white rounded-lg shadow-md p-4 mb-4">
        {messages && messages.length > 0 ? (
          <div className="space-y-4">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.senderId === user?.id ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                    message.senderId === user?.id ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-900'
                  }`}
                >
                  {message.text && <p>{message.text}</p>}
                  {message.mediaUrl && (
                    <img src={message.mediaUrl} alt="Media" className="mt-2 rounded max-w-full" />
                  )}
                  <p className="text-xs mt-1 opacity-75">
                    {format(new Date(message.createdAt), 'h:mm a')}
                  </p>
                </div>
              </div>
            ))}
            <div ref={messagesEndRef} />
          </div>
        ) : (
          <div className="text-center text-gray-500 py-8">No messages yet. Start the conversation!</div>
        )}
      </div>
      <form onSubmit={handleSend} className="flex space-x-2">
        <input
          type="text"
          className="flex-1 px-4 py-2 border border-gray-300 rounded-md"
          placeholder="Type a message..."
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <input
          type="file"
          accept="image/*,video/*"
          className="hidden"
          id="file-input"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />
        <label
          htmlFor="file-input"
          className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 cursor-pointer"
        >
          📎
        </label>
        <button
          type="submit"
          disabled={sendMessage.isPending || (!text && !file)}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
        >
          Send
        </button>
      </form>
    </div>
  );
}

