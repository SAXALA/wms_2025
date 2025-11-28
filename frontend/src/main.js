import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import router from './router';
import App from './App.vue';
import './styles/global.css';

const app = createApp(App);

Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
    app.component(name, component);
});

app.use(ElementPlus);
app.use(router);
app.mount('#app');
