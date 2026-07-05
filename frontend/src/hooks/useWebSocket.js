import { useEffect, useRef, useState } from 'react'

export default function useWebSocket(url) {
  const [messages, setMessages] = useState([])
  const socketRef = useRef(null)

  useEffect(() => {
    const socket = new WebSocket(url)
    socketRef.current = socket

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        setMessages((prev) => [data, ...prev].slice(0, 100))
      } catch {
        // ignore malformed messages
      }
    }

    return () => socket.close()
  }, [url])

  return messages
}
