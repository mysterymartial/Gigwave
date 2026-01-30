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
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-teal-500"></div>
          <p className="text-gray-400 mt-4">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 py-4 sm:py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto h-[calc(100vh-120px)] sm:h-[calc(100vh-200px)] flex flex-col">
        <h1 className="text-xl sm:text-2xl font-bold text-white mb-3 sm:mb-4">Chat</h1>
        <div className="flex-1 overflow-y-auto bg-emerald-900/40 dark:bg-emerald-900/50 rounded-xl border border-emerald-700/40 shadow-lg p-3 sm:p-4 mb-3 sm:mb-4">
          {messages && messages.length > 0 ? (
            <div className="space-y-4">
              {messages.map((message) => (
                <div
                  key={message.id}
                  className={`flex ${message.senderId === user?.id ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                      message.senderId === user?.id 
                        ? 'bg-teal-500 text-white' 
                        : 'bg-gray-800/80 text-emerald-200'
                    }`}
                  >
                    {message.text && <p>{message.text}</p>}
                    {message.mediaUrl && (
                      <img src={message.mediaUrl} alt="Media" className="mt-2 rounded max-w-full" />
                    )}
                    <p className={`text-xs mt-1 ${message.senderId === user?.id ? 'opacity-75' : 'text-emerald-200/60'}`}>
                      {format(new Date(message.createdAt), 'h:mm a')}
                    </p>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
          ) : (
            <div className="text-center text-gray-400 py-8">No messages yet. Start the conversation!</div>
          )}
        </div>
        <form onSubmit={handleSend} className="flex flex-col sm:flex-row gap-2 sm:space-x-2">
          <input
            type="text"
            className="flex-1 px-3 sm:px-4 py-2 sm:py-3 bg-gray-800/80 border border-emerald-700/50 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 text-sm sm:text-base"
            placeholder="Type a message..."
            value={text}
            onChange={(e) => setText(e.target.value)}
          />
          <div className="flex gap-2">
            <input
              type="file"
              accept="image/*,video/*"
              className="hidden"
              id="file-input"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />
            <label
              htmlFor="file-input"
              className="px-3 sm:px-4 py-2 sm:py-3 bg-emerald-500 hover:bg-emerald-400 text-white rounded-lg cursor-pointer transition-colors font-medium text-sm sm:text-base"
            >
              📎
            </label>
            <button
              type="submit"
              disabled={sendMessage.isPending || (!text && !file)}
              className="px-4 sm:px-6 py-2 sm:py-3 bg-teal-500 hover:bg-teal-400 text-white rounded-lg disabled:opacity-50 transition-colors font-semibold text-sm sm:text-base"
            >
              Send
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

