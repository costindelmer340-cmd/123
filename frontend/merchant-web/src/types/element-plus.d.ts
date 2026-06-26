declare module 'element-plus' {
  import type { App, Plugin } from 'vue'
  const ElementPlus: Plugin
  export default ElementPlus
  export function ElMessage(...args: unknown[]): void
}
