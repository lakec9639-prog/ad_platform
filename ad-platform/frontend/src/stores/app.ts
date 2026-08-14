import { defineStore } from 'pinia'
import { ref } from 'vue'
import dayjs from 'dayjs'

export const useAppStore = defineStore('app', () => {
  // Default date range: last 30 days
  const dateRange = ref<[Date, Date]>([
    dayjs().subtract(30, 'day').toDate(),
    dayjs().toDate(),
  ])

  function setDateRange(range: [Date, Date]) {
    dateRange.value = range
  }

  return {
    dateRange,
    setDateRange,
  }
})
