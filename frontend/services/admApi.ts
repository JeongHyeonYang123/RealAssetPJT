// src/services/admApi.ts
import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api/v1'

export const admApi = {
    // 시도 리스트 조회
    async getSidoList(): Promise<any> {
        try {
            console.log('📍 시도 API 호출 시작:', `${API_BASE_URL}/adm/sido`);
            const response = await axios.get(`${API_BASE_URL}/adm/sido`);
            console.log('📍 시도 API 원시 응답:', response);
            console.log('📍 시도 API 데이터:', response.data);
            return response.data;
        } catch (error) {
            console.error('📍 시도 API 에러:', error);
            throw error;
        }
    },

    // 구군 리스트 조회
    async getGugunList(sidoCode: string): Promise<any> {
        try {
            console.log('🏙️ 구군 API 호출:', sidoCode);
            const response = await axios.get(`${API_BASE_URL}/adm/gugun`, {
                params: {sidoCode}
            });
            console.log('🏙️ 구군 API 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('🏙️ 구군 API 에러:', error);
            throw error;
        }
    },

    // 읍면동 리스트 조회
    async getDongList(gugunCode: string): Promise<any> {
        try {
            console.log('🏘️ 읍면동 API 호출:', gugunCode);
            const response = await axios.get(`${API_BASE_URL}/adm/dong`, {
                params: {gugunCode}
            });
            console.log('🏘️ 읍면동 API 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('🏘️ 읍면동 API 에러:', error);
            throw error;
        }
    },

    // 동코드 정보 조회
    async getDongCode(dongCode: string): Promise<any> {
        try {
            console.log('📍 동코드 API 호출:', dongCode);
            const response = await axios.get(`${API_BASE_URL}/adm/dongcode`, {
                params: {dongCode}
            });
            console.log('📍 동코드 API 응답:', response.data);
            return response.data;
        } catch (error) {
            console.error('📍 동코드 API 에러:', error);
            throw error;
        }
    }
}
