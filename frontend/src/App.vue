<template>
    <div class="min-h-screen flex flex-col">
        <app-header
            class="sticky top-0 z-50 bg-white border-b border-gray-200"
        />
        <main class="flex-1">
            <router-view v-slot="{ Component, route }">
                <transition name="fade" mode="out-in" appear>
                    <keep-alive>
                        <component :is="Component" :key="route.fullPath" />
                    </keep-alive>
                </transition>
            </router-view>
        </main>
    </div>
</template>

<script setup lang="ts">
import AppHeader from "./components/layout/AppHeader.vue";
</script>

<style>
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

body {
    margin: 0;
    padding: 0;
    min-height: 100vh;
}

#app {
    min-height: 100vh;
}

/* 공통 스타일 */
.container {
    @apply max-w-7xl mx-auto px-4 sm:px-6 lg:px-8;
}

/* 클릭 시 마커 흔들기 */
@keyframes marker-bounce {
    0% {
        transform: translateY(0);
    }
    20% {
        transform: translateY(-20px);
    }
    40% {
        transform: translateY(0);
    }
    60% {
        transform: translateY(-10px);
    }
    80% {
        transform: translateY(0);
    }
    100% {
        transform: translateY(0);
    }
}

.marker-bounce {
    animation: marker-bounce 0.6s;
}

.card {
    @apply bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow duration-200;
}

.btn {
    @apply px-4 py-2 rounded-lg font-medium transition-colors duration-200;
}

.btn-primary {
    @apply bg-primary text-white hover:bg-primary/90;
}

.btn-outline {
    @apply border border-gray-300 text-gray-700 hover:bg-gray-50;
}

.input-field {
    @apply w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none;
}
</style>
