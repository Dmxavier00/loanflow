export default function MessageBanner({ type = 'info', children }) {
  if (!children) {
    return null;
  }

  return <div className={`message-banner message-${type}`}>{children}</div>;
}
