/** .vue 单文件组件模块声明（HBuilderX 编译时由内置编译器处理） */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}
