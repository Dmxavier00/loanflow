import { useId } from 'react';

export default function Logo({ size = 30 }) {
  const id = useId();
  const shellGradient = `${id}-shell`;
  const crestGradient = `${id}-crest`;
  const coreGradient = `${id}-core`;
  const flowGradient = `${id}-flow`;

  return (
    <svg width={size} height={size} viewBox="0 0 72 72" aria-hidden="true">
      <defs>
        <linearGradient id={shellGradient} x1="14" y1="12" x2="58" y2="58" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#2d73ff" />
          <stop offset="0.48" stopColor="#1847c8" />
          <stop offset="1" stopColor="#060d1b" />
        </linearGradient>
        <linearGradient id={crestGradient} x1="18" y1="16" x2="47" y2="42" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="0.12" stopColor="#d9e6ff" />
          <stop offset="1" stopColor="#0a1738" />
        </linearGradient>
        <linearGradient id={coreGradient} x1="33" y1="18" x2="58" y2="53" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#0f2d8f" />
          <stop offset="0.52" stopColor="#091838" />
          <stop offset="1" stopColor="#04070f" />
        </linearGradient>
        <linearGradient id={flowGradient} x1="15" y1="48" x2="41" y2="34" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#1a4fd7" />
          <stop offset="1" stopColor="#2f7fff" />
        </linearGradient>
      </defs>

      <path
        d="M36 4.5 57 16.7v28.6L36 57.5 15 45.3V16.7L36 4.5Z"
        fill={`url(#${shellGradient})`}
      />
      <path
        d="M28.4 14.7 18.3 23v17.3c0 2.6 1.2 5 3.3 6.5l2.7 1.9c2 1.4 4.8 0 4.8-2.5V31.2l15.1-11c3.8-2.7 8.8-3.1 13-1l-8.1-5.5c-4.3-2.9-10-2.5-14.7.9Z"
        fill={`url(#${crestGradient})`}
      />
      <path
        d="M53.7 23.4v18.4c0 3.1-1.5 6-4.1 7.8L35.3 60.1c-2.4 1.7-5.7 0-5.7-2.9v-8.6l14.6-10.3c2.8-2 6.3-2.9 9.5-2.6Z"
        fill={`url(#${coreGradient})`}
      />
      <path
        d="M12.6 49.1c5.9-7.8 14.1-12.9 23.6-14.6l-9.1 7.9c-3.9 3.4-8.3 6.1-13 8.3l-1.5-1.6Z"
        fill={`url(#${flowGradient})`}
      />
    </svg>
  );
}
