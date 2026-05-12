export default function UiIcon({ name, className = '', size = 20, strokeWidth = 1.8 }) {
  const sharedProps = {
    className,
    width: size,
    height: size,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    'aria-hidden': 'true'
  };

  switch (name) {
    case 'home':
      return (
        <svg {...sharedProps}>
          <path d="M3.5 10.5L12 3l8.5 7.5" />
          <path d="M5.5 9.5V20h13V9.5" />
          <path d="M9.5 20v-6h5v6" />
        </svg>
      );

    case 'file':
      return (
        <svg {...sharedProps}>
          <path d="M8 3.5h6l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 20V5A1.5 1.5 0 0 1 7.5 3.5H8Z" />
          <path d="M14 3.5V8h4.5" />
          <path d="M9 12h6" />
          <path d="M9 16h6" />
        </svg>
      );

    case 'file-check':
      return (
        <svg {...sharedProps}>
          <path d="M8 3.5h6l4 4V20a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 20V5A1.5 1.5 0 0 1 7.5 3.5H8Z" />
          <path d="M14 3.5V8h4.5" />
          <path d="m9.2 14 1.9 1.9 3.7-3.9" />
        </svg>
      );

    case 'id-card':
      return (
        <svg {...sharedProps}>
          <rect x="4" y="4.5" width="16" height="15" rx="2.5" />
          <circle cx="9" cy="10" r="1.8" />
          <path d="M6.8 15c.5-1.4 1.3-2.2 2.2-2.2.9 0 1.7.8 2.2 2.2" />
          <path d="M14 9h3.2" />
          <path d="M14 12h3.2" />
          <path d="M14 15h2.2" />
        </svg>
      );

    case 'signature':
      return (
        <svg {...sharedProps}>
          <path d="M4 18.5h7.5" />
          <path d="M6 15.5L15.5 6a2.1 2.1 0 1 1 3 3l-9.5 9.5-3 1Z" />
          <path d="M13 8.5l2.5 2.5" />
          <path d="M14.5 18.5c1.2-2.1 2.6-3.2 4.5-3.2 1 0 1.6.3 2 .7" />
        </svg>
      );

    case 'wallet':
      return (
        <svg {...sharedProps}>
          <rect x="3" y="6.5" width="18" height="11" rx="2.5" />
          <path d="M3 10.5h18" />
          <path d="M16 14h2.5" />
        </svg>
      );

    case 'user':
      return (
        <svg {...sharedProps}>
          <circle cx="12" cy="8" r="3.2" />
          <path d="M5 19c1.6-3 4-4.5 7-4.5s5.4 1.5 7 4.5" />
        </svg>
      );

    case 'bank':
      return (
        <svg {...sharedProps}>
          <path d="M3.5 9.5 12 4l8.5 5.5" />
          <path d="M5.5 10.5h13" />
          <path d="M7 10.5v7" />
          <path d="M12 10.5v7" />
          <path d="M17 10.5v7" />
          <path d="M4.5 18.5h15" />
        </svg>
      );

    case 'shield-check':
      return (
        <svg {...sharedProps}>
          <path d="M12 3.5 18.5 6v5.2c0 4.1-2.4 7-6.5 9.3-4.1-2.3-6.5-5.2-6.5-9.3V6L12 3.5Z" />
          <path d="m9.4 12 1.8 1.8 3.6-3.8" />
        </svg>
      );

    case 'clock':
      return (
        <svg {...sharedProps}>
          <circle cx="12" cy="12" r="8.5" />
          <path d="M12 7.5v5l3.5 2" />
        </svg>
      );

    case 'edit':
      return (
        <svg {...sharedProps}>
          <path d="M4.5 19.5h4.2l9.3-9.3a2.1 2.1 0 0 0-3-3l-9.3 9.3Z" />
          <path d="m13.8 8.4 2.8 2.8" />
        </svg>
      );

    case 'help':
      return (
        <svg {...sharedProps}>
          <circle cx="12" cy="12" r="9" />
          <path d="M9.8 9.6a2.4 2.4 0 1 1 4.1 2c-.7.6-1.4 1-1.9 1.7-.3.4-.4.8-.4 1.4" />
          <circle cx="12" cy="17.2" r=".6" fill="currentColor" stroke="none" />
        </svg>
      );

    case 'logout':
      return (
        <svg {...sharedProps}>
          <path d="M10 4.5H6.5A1.5 1.5 0 0 0 5 6v12a1.5 1.5 0 0 0 1.5 1.5H10" />
          <path d="M13 8.5l4 3.5-4 3.5" />
          <path d="M17 12H9" />
        </svg>
      );

    case 'bell':
      return (
        <svg {...sharedProps}>
          <path d="M8.5 18.5h7" />
          <path d="M7 15.5V11a5 5 0 1 1 10 0v4.5l1.2 1.5H5.8L7 15.5Z" />
          <path d="M10 18.5a2 2 0 0 0 4 0" />
        </svg>
      );

    case 'chevron-down':
      return (
        <svg {...sharedProps}>
          <path d="M7.5 9.5 12 14l4.5-4.5" />
        </svg>
      );

    case 'currency':
      return (
        <svg {...sharedProps}>
          <path d="M14.8 7.4c-.6-.8-1.6-1.4-2.8-1.4-1.8 0-3.2 1.1-3.2 2.6 0 1.4 1 2.1 3.2 2.7 2.1.6 3.2 1.3 3.2 2.7 0 1.5-1.4 2.6-3.2 2.6-1.4 0-2.5-.5-3.3-1.6" />
          <path d="M12 4v16" />
        </svg>
      );

    case 'calendar':
      return (
        <svg {...sharedProps}>
          <rect x="4" y="5.5" width="16" height="14" rx="2.5" />
          <path d="M8 3.5v4" />
          <path d="M16 3.5v4" />
          <path d="M4 9.5h16" />
        </svg>
      );

    case 'stack':
      return (
        <svg {...sharedProps}>
          <ellipse cx="12" cy="6.5" rx="6.5" ry="2.7" />
          <path d="M5.5 6.5v4c0 1.5 2.9 2.7 6.5 2.7s6.5-1.2 6.5-2.7v-4" />
          <path d="M5.5 10.5v4c0 1.5 2.9 2.7 6.5 2.7s6.5-1.2 6.5-2.7v-4" />
        </svg>
      );

    case 'chart':
      return (
        <svg {...sharedProps}>
          <path d="M4.5 18.5h15" />
          <path d="M6.5 15l3.5-3.5 3 2.5 4.5-5" />
          <circle cx="6.5" cy="15" r="1" />
          <circle cx="10" cy="11.5" r="1" />
          <circle cx="13" cy="14" r="1" />
          <circle cx="17.5" cy="9" r="1" />
        </svg>
      );

    case 'arrow-right':
      return (
        <svg {...sharedProps}>
          <path d="M5 12h13" />
          <path d="m13 7 5 5-5 5" />
        </svg>
      );

    default:
      return (
        <svg {...sharedProps}>
          <circle cx="12" cy="12" r="8.5" />
        </svg>
      );
  }
}
