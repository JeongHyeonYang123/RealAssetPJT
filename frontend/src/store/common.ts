import {acceptHMRUpdate, defineStore} from 'pinia'
import {computed, ref} from 'vue'

export const useCommonStore = defineStore('common', () => {
    const _taskCnt = ref<number>(0)
    const hasMoreTask = computed<boolean>(() => _taskCnt.value > 0)

    const addTask = (): void => {
        _taskCnt.value++
    }

    const removeTask = (): void => {
        _taskCnt.value--
    }

    return { hasMoreTask, addTask, removeTask }
})

if (import.meta.hot) {
    import.meta.hot.accept(acceptHMRUpdate(useCommonStore, import.meta.hot))
}
