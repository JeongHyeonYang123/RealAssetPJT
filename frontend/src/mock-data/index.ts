import {Property, Transaction} from '../types'

// Mock Property Data
export const mockProperties: Property[] = [
  {
    id: 1,
    name: '래미안 아파트',
    address: '서울특별시 강남구 테헤란로 123',
    lat: 37.5047,
    lng: 127.0507,
    recentPrice: 130000, // 1억 3천만원
    recentTransaction: '2023-12-15',
    averageArea: 84.3,
    builtYear: 2015,
    totalUnits: 320,
    transactions: []
  },
  {
    id: 2,
    name: '자이 아파트',
    address: '서울특별시 서초구 서초대로 456',
    lat: 37.4969,
    lng: 127.0278,
    recentPrice: 185000, // 1억 8천 5백만원
    recentTransaction: '2023-11-20',
    averageArea: 115.7,
    builtYear: 2018,
    totalUnits: 250,
    transactions: []
  },
  {
    id: 3,
    name: '힐스테이트 아파트',
    address: '서울특별시 송파구 올림픽로 789',
    lat: 37.5140,
    lng: 127.1060,
    recentPrice: 160000, // 1억 6천만원
    recentTransaction: '2023-12-05',
    averageArea: 98.6,
    builtYear: 2017,
    totalUnits: 280,
    transactions: []
  },
  {
    id: 4,
    name: 'e편한세상 아파트',
    address: '서울특별시 강서구 마곡중앙로 101',
    lat: 37.5668,
    lng: 126.8293,
    recentPrice: 95000, // 9천 5백만원
    recentTransaction: '2023-11-10',
    averageArea: 72.8,
    builtYear: 2016,
    totalUnits: 210,
    transactions: []
  },
  {
    id: 5,
    name: '더샵 아파트',
    address: '서울특별시 동대문구 천호대로 555',
    lat: 37.5748,
    lng: 127.0395,
    recentPrice: 75000, // 7천 5백만원
    recentTransaction: '2023-12-18',
    averageArea: 59.5,
    builtYear: 2014,
    totalUnits: 180,
    transactions: []
  },
  {
    id: 6,
    name: '푸르지오 아파트',
    address: '서울특별시 영등포구 여의대로 333',
    lat: 37.5255,
    lng: 126.9348,
    recentPrice: 140000, // 1억 4천만원
    recentTransaction: '2023-10-25',
    averageArea: 105.2,
    builtYear: 2019,
    totalUnits: 300,
    transactions: []
  },
  {
    id: 7,
    name: '롯데캐슬 아파트',
    address: '서울특별시 노원구 노원로 222',
    lat: 37.6545,
    lng: 127.0698,
    recentPrice: 65000, // 6천 5백만원
    recentTransaction: '2023-11-30',
    averageArea: 68.4,
    builtYear: 2013,
    totalUnits: 150,
    transactions: []
  },
  {
    id: 8,
    name: '아크로 아파트',
    address: '서울특별시 성동구 뚝섬로 777',
    lat: 37.5472,
    lng: 127.0437,
    recentPrice: 220000, // 2억 2천만원
    recentTransaction: '2023-12-10',
    averageArea: 132.5,
    builtYear: 2020,
    totalUnits: 180,
    transactions: []
  },
  {
    id: 9,
    name: '센트럴파크 아파트',
    address: '서울특별시 강남구 삼성로 888',
    lat: 37.5123,
    lng: 127.0568,
    recentPrice: 250000, // 2억 5천만원
    recentTransaction: '2023-12-20',
    averageArea: 145.8,
    builtYear: 2021,
    totalUnits: 400,
    transactions: []
  },
  {
    id: 10,
    name: '해운대 아파트',
    address: '서울특별시 송파구 잠실로 999',
    lat: 37.5087,
    lng: 127.1128,
    recentPrice: 180000, // 1억 8천만원
    recentTransaction: '2023-12-12',
    averageArea: 112.3,
    builtYear: 2019,
    totalUnits: 280,
    transactions: []
  }
]

// Mock Transaction Data
export const mockTransactions: Transaction[] = [
  {
    id: 101,
    propertyId: 1,
    propertyName: '래미안 아파트',
    address: '서울특별시 강남구 테헤란로 123',
    price: 130000, // 1억 3천만원
    date: '2023-12-15',
    floor: 15,
    area: 84.3,
    builtYear: 2015,
    lat: 37.5047,
    lng: 127.0507
  },
  {
    id: 102,
    propertyId: 1,
    propertyName: '래미안 아파트',
    address: '서울특별시 강남구 테헤란로 123',
    price: 125000, // 1억 2천 5백만원
    date: '2023-11-05',
    floor: 10,
    area: 84.3,
    builtYear: 2015,
    lat: 37.5047,
    lng: 127.0507
  },
  {
    id: 103,
    propertyId: 1,
    propertyName: '래미안 아파트',
    address: '서울특별시 강남구 테헤란로 123',
    price: 128000, // 1억 2천 8백만원
    date: '2023-10-20',
    floor: 12,
    area: 84.3,
    builtYear: 2015,
    lat: 37.5047,
    lng: 127.0507
  },
  {
    id: 201,
    propertyId: 2,
    propertyName: '자이 아파트',
    address: '서울특별시 서초구 서초대로 456',
    price: 185000, // 1억 8천 5백만원
    date: '2023-11-20',
    floor: 20,
    area: 115.7,
    builtYear: 2018,
    lat: 37.4969,
    lng: 127.0278
  },
  {
    id: 202,
    propertyId: 2,
    propertyName: '자이 아파트',
    address: '서울특별시 서초구 서초대로 456',
    price: 180000, // 1억 8천만원
    date: '2023-10-15',
    floor: 18,
    area: 115.7,
    builtYear: 2018,
    lat: 37.4969,
    lng: 127.0278
  },
  {
    id: 301,
    propertyId: 3,
    propertyName: '힐스테이트 아파트',
    address: '서울특별시 송파구 올림픽로 789',
    price: 160000, // 1억 6천만원
    date: '2023-12-05',
    floor: 16,
    area: 98.6,
    builtYear: 2017,
    lat: 37.5140,
    lng: 127.1060
  },
  {
    id: 401,
    propertyId: 4,
    propertyName: 'e편한세상 아파트',
    address: '서울특별시 강서구 마곡중앙로 101',
    price: 95000, // 9천 5백만원
    date: '2023-11-10',
    floor: 9,
    area: 72.8,
    builtYear: 2016,
    lat: 37.5668,
    lng: 126.8293
  },
  {
    id: 402,
    propertyId: 4,
    propertyName: 'e편한세상 아파트',
    address: '서울특별시 강서구 마곡중앙로 101',
    price: 92000, // 9천 2백만원
    date: '2023-09-25',
    floor: 7,
    area: 72.8,
    builtYear: 2016,
    lat: 37.5668,
    lng: 126.8293
  },
  {
    id: 501,
    propertyId: 5,
    propertyName: '더샵 아파트',
    address: '서울특별시 동대문구 천호대로 555',
    price: 75000, // 7천 5백만원
    date: '2023-12-18',
    floor: 8,
    area: 59.5,
    builtYear: 2014,
    lat: 37.5748,
    lng: 127.0395
  },
  {
    id: 601,
    propertyId: 6,
    propertyName: '푸르지오 아파트',
    address: '서울특별시 영등포구 여의대로 333',
    price: 140000, // 1억 4천만원
    date: '2023-10-25',
    floor: 15,
    area: 105.2,
    builtYear: 2019,
    lat: 37.5255,
    lng: 126.9348
  },
  {
    id: 701,
    propertyId: 7,
    propertyName: '롯데캐슬 아파트',
    address: '서울특별시 노원구 노원로 222',
    price: 65000, // 6천 5백만원
    date: '2023-11-30',
    floor: 7,
    area: 68.4,
    builtYear: 2013,
    lat: 37.6545,
    lng: 127.0698
  },
  {
    id: 801,
    propertyId: 8,
    propertyName: '아크로 아파트',
    address: '서울특별시 성동구 뚝섬로 777',
    price: 220000, // 2억 2천만원
    date: '2023-12-10',
    floor: 22,
    area: 132.5,
    builtYear: 2020,
    lat: 37.5472,
    lng: 127.0437
  },
  {
    id: 901,
    propertyId: 9,
    propertyName: '센트럴파크 아파트',
    address: '서울특별시 강남구 삼성로 888',
    price: 250000, // 2억 5천만원
    date: '2023-12-20',
    floor: 25,
    area: 145.8,
    builtYear: 2021,
    lat: 37.5123,
    lng: 127.0568
  },
  {
    id: 902,
    propertyId: 9,
    propertyName: '센트럴파크 아파트',
    address: '서울특별시 강남구 삼성로 888',
    price: 245000, // 2억 4천 5백만원
    date: '2023-11-15',
    floor: 20,
    area: 145.8,
    builtYear: 2021,
    lat: 37.5123,
    lng: 127.0568
  },
  {
    id: 1001,
    propertyId: 10,
    propertyName: '해운대 아파트',
    address: '서울특별시 송파구 잠실로 999',
    price: 180000, // 1억 8천만원
    date: '2023-12-12',
    floor: 18,
    area: 112.3,
    builtYear: 2019,
    lat: 37.5087,
    lng: 127.1128
  },
  {
    id: 1002,
    propertyId: 10,
    propertyName: '해운대 아파트',
    address: '서울특별시 송파구 잠실로 999',
    price: 175000, // 1억 7천 5백만원
    date: '2023-11-25',
    floor: 15,
    area: 112.3,
    builtYear: 2019,
    lat: 37.5087,
    lng: 127.1128
  }
]
