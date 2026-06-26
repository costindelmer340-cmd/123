import { ElMessage } from 'element-plus'

export function toastTodo(action: string) {
  ElMessage({ type: 'info', message: `${action}功能正在接入中` })
}
