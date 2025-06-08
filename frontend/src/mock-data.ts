import { Property, Transaction } from './types'

export const mockProperties: Property[] = [
  {
    id: 1,
    aptSeq: '123456',
    name: '서울 아파트',
    address: '서울특별시 강남구 역삼동',
    type: '아파트',
    area: 82.5,
    floor: 15,
    totalFloors: 20,
    builtYear: 2010,
    price: 1000000000,
    maintenanceFee: 300000,
    parking: true,
    heatingType: '지역난방'
  },
  {
    id: 2,
    aptSeq: '789012',
    name: '분당 빌라',
    address: '경기도 성남시 분당구 정자동',
    type: '빌라',
    area: 60.0,
    floor: 2,
    totalFloors: 3,
    builtYear: 2005,
    price: 500000000,
    maintenanceFee: 150000,
    parking: false,
    heatingType: '개별난방'
  },
  {
    id: 3,
    aptSeq: '345678',
    name: '인천 오피스텔',
    address: '인천광역시 연수구 송도동',
    type: '오피스텔',
    area: 45.0,
    floor: 12,
    totalFloors: 25,
    builtYear: 2015,
    price: 700000000,
    maintenanceFee: 250000,
    parking: true,
    heatingType: '지역난방'
  }
]

export const mockTransactions: Transaction[] = [
  {
    id: 1,
    propertyId: 1,
    type: '매매',
    price: 950000000,
    date: '2025-01-15',
    floor: 15,
    area: 82.5
  },
  {
    id: 2,
    propertyId: 1,
    type: '전세',
    price: 700000000,
    date: '2024-11-20',
    floor: 10,
    area: 82.5
  },
  {
    id: 3,
    propertyId: 2,
    type: '매매',
    price: 450000000,
    date: '2025-02-05',
    floor: 2,
    area: 60.0
  },
  {
    id: 4,
    propertyId: 3,
    type: '전세',
    price: 500000000,
    date: '2024-12-10',
    floor: 12,
    area: 45.0
  }
]
