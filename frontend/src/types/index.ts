export interface Property {
  id: number
  name: string
  address: string
  lat: number
  lng: number
  recentPrice: number
  recentTransaction?: string // ISO date string
  averageArea?: number
  builtYear: number
  totalUnits: number
  transactions: Transaction[]
  isDong?: boolean
  aptCount?: number
  aptSeq?: string
}

export interface Transaction {
  id: number
  propertyId: number
  propertyName: string
  address: string
  price: number
  date: string
  floor: number
  area: number
  builtYear: number
  lat: number
  lng: number
  aptSeq: string
}