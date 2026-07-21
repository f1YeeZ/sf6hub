<template>
  <a
    class="social-media-link"
    href="https://v.douyin.com/mkCr70KoRhw/"
    target="_blank"
    rel="noopener noreferrer"
    title="我的抖音"
    aria-label="访问我的抖音主页（新窗口）"
  >
    <svg class="douyin-mark" viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12.53.02c1.31-.02 2.61-.01 3.91-.02.08 1.53.63 3.09 1.75 4.17 1.12 1.11 2.7 1.62 4.24 1.79v4.03c-1.44-.05-2.89-.35-4.2-.97-.57-.26-1.1-.59-1.62-.93-.01 2.92.01 5.84-.02 8.75-.08 1.4-.54 2.79-1.35 3.94-1.31 1.92-3.58 3.17-5.91 3.21-1.43.08-2.86-.31-4.08-1.03-2.02-1.19-3.44-3.37-3.65-5.72-.02-.5-.03-1-.01-1.49.18-1.9 1.12-3.75 2.57-4.99 1.35-1.17 3.13-1.83 4.92-1.79.03 1.48-.04 2.96-.04 4.44-1.01-.33-2.19-.24-3.08.35-.64.41-1.13 1.04-1.39 1.75-.22.51-.16 1.07-.15 1.61.24 1.64 1.82 3.02 3.5 2.87 1.11-.01 2.17-.66 2.75-1.61.19-.33.4-.67.41-1.06.1-1.79.06-3.57.07-5.36.01-4.03-.01-8.05.02-12.07z" />
    </svg>
    <span class="sr-only">我的抖音</span>
  </a>
</template>

<style scoped>
.social-media-link {
  position: fixed;
  right: 18px;
  bottom: calc(18px + env(safe-area-inset-bottom, 0px));
  z-index: 20;
  width: 44px;
  height: 44px;
  display: grid;
  align-items: center;
  justify-items: center;
  color: rgba(255, 248, 239, 0.68);
  background: rgba(7, 5, 6, 0.42);
  border: 1px solid rgba(255, 248, 239, 0.09);
  border-radius: 50%;
  opacity: 0.72;
  text-decoration: none;
  outline: none;
  transition: color 0.18s ease, opacity 0.18s ease, background 0.18s ease, border-color 0.18s ease, transform 0.18s cubic-bezier(0.16, 1, 0.3, 1);
}

.social-media-link:hover,
.social-media-link:focus-visible {
  color: #fff8ef;
  background: rgba(18, 16, 15, 0.82);
  border-color: rgba(49, 213, 230, 0.34);
  opacity: 1;
  transform: translateY(-1px);
}

.social-media-link:focus-visible {
  box-shadow: 0 0 0 3px rgba(49, 213, 230, 0.16);
}

.douyin-mark {
  width: 20px;
  height: 20px;
  overflow: visible;
  fill: currentColor;
  filter: drop-shadow(-1px 0 0 rgba(37, 244, 238, 0.38)) drop-shadow(1px 0 0 rgba(254, 44, 85, 0.38));
  transition: filter 0.18s ease;
}

.social-media-link:hover .douyin-mark,
.social-media-link:focus-visible .douyin-mark {
  filter: drop-shadow(-1.4px 0 0 #25f4ee) drop-shadow(1.4px 0 0 #fe2c55);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 560px) {
  .social-media-link {
    right: 12px;
    bottom: calc(12px + env(safe-area-inset-bottom, 0px));
    width: 42px;
    height: 42px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .social-media-link {
    transition: color 0.18s ease, opacity 0.18s ease, background 0.18s ease, border-color 0.18s ease;
  }

  .social-media-link:hover,
  .social-media-link:focus-visible {
    transform: none;
  }
}
</style>
