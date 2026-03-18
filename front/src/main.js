import { createApp } from "vue"
import { createPinia } from "pinia"
import App from "./App.vue"
import router from "./router"
import ElementPlus from "element-plus"
import "element-plus/dist/index.css"
import "@/assets/main.css" // 引入全局优化样式
import permissionDirective from "./utils/permission" // 引入自定义权限指令

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册全局指令 v-permission
app.directive('permission', permissionDirective)

app.mount("#app")
