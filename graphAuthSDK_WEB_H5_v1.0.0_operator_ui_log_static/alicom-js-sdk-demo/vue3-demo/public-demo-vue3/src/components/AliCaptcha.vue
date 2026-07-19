<!--
 * @Description: 
 * @Version: 1.0.0
 * @Date: 2022-04-11 10:16:14
 * @LastEditors: yawen Yang
 * @LastEditTime: 2024-03-06 17:48:19
-->
<template>
  <div class="captcha"></div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "AliCaptcha",
  props: ["captchaConfig"],
  setup(prop) {
    (window as any).initAlicom4(
      prop.captchaConfig.config,
      prop.captchaConfig.handler
    );
  },
});
</script>
