// services/propertyApi.ts
import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api/v1'

export interface PropertySearchParams {
    dongCode: string
    aptName: string
    priceFilter?: string
    areaFilter?: string
    sortOrder?: string
    page?: number
    size?: number
}

export interface HouseDeal {
    no: number
    aptSeq: string
    aptDong: string
    floor: number
    dealYear: number
    dealMonth: number
    dealDay: number
    excluUseAr: number
    dealAmount: string
    aptNm: string
    umdNm?: string
    buildYear?: number
}

export interface PropertySearchResponse {
    properties: HouseDeal[]
    totalCount: number
    totalPages: number
    currentPage: number
}

export interface ApiResponse<T> {
    success: boolean
    message: string
    data: T
}

export const propertyApi = {
    // 아파트 거래내역 검색
    async searchProperties(params: PropertySearchParams): Promise<ApiResponse<PropertySearchResponse>> {
        const response = await axios.get(`${API_BASE_URL}/transactions/search`, {
            params: {
                dongCode: params.dongCode,
                aptName: params.aptName,
                priceFilter: params.priceFilter || 'all',
                areaFilter: params.areaFilter || 'all',
                sortOrder: params.sortOrder || 'date-desc',
                page: params.page || 1,
                size: params.size || 10
            }
        })
        return response.data
    },

    // 필터 옵션 조회
    async getFilterOptions(): Promise<ApiResponse<any>> {
        const response = await axios.get(`${API_BASE_URL}/transactions/filters`)
        return response.data
    }
}
