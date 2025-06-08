import {defineStore} from 'pinia'
import {ref} from 'vue'
import {Property, Transaction} from '../types'
import {mockProperties, mockTransactions} from '../mock-data'

export const usePropertyStore = defineStore('property', () => {
  const searchResults = ref<Property[]>([])
  const recentTransactions = ref<Transaction[]>([])
  const currentProperty = ref<Property | null>(null)
  
  async function searchProperties(query: string): Promise<void> {
    // In a real app, this would call an API
    // For demo purposes, we're using mock data
    
    // Simulate API call delay
    await new Promise(resolve => setTimeout(resolve, 800))
    
    // Filter properties based on query
    const normalizedQuery = query.toLowerCase().trim()
    
    searchResults.value = mockProperties.filter(property => 
      property.name.toLowerCase().includes(normalizedQuery) || 
      property.address.toLowerCase().includes(normalizedQuery)
    )
  }
  
  async function fetchRecentTransactions(): Promise<void> {
    // Simulate API call delay
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // In a real app, this would fetch from an API
    recentTransactions.value = mockTransactions
  }
  
  async function fetchPropertyDetail(propertyId: string): Promise<void> {
    // Simulate API call delay
    await new Promise(resolve => setTimeout(resolve, 700))
    
    // aptSeq로만 찾기
    const property = mockProperties.find(p => p.aptSeq === propertyId);
    
    if (property) {
      property.transactions = mockTransactions.filter(t => t.aptSeq === propertyId);
      currentProperty.value = property;
    } else {
      throw new Error('Property not found');
    }
  }
  
  return {
    searchResults,
    recentTransactions,
    currentProperty,
    searchProperties,
    fetchRecentTransactions,
    fetchPropertyDetail
  }
})