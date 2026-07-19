/*
 * @Date: 2024-03-06 17:45:26
 * @LastEditors: yawen Yang
 * @LastEditTime: 2024-03-06 17:47:52
 * @FilePath: /aly/gt4-public-client-demo/src/shims-vue.d.ts
 */
import axios from "axios";
/* eslint-disable */
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface Winodw {
  initAlicom4: any
}
