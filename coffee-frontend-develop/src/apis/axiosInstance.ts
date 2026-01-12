import { QueryClient } from '@tanstack/react-query';
import axios from 'axios';

import { getCookies, removeCookies } from './AppUser/cookie';

export const queryClient = new QueryClient();
// const baseURL =
//     import.meta.env.MODE === 'development'
//         ? '/api' // 개발 시 프록시 경로
//         : import.meta.env.VITE_API_URL;

export const axiosInstance = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

axiosInstance.interceptors.request.use(
    (config) => {
        // 토큰 가져오기
        const token = getCookies('accessToken');
        // const token =
        // 'eyJhbGciOiJIUzUxMiJ9.eyJhcHBVc2VySWQiOiIxIiwiaWF0IjoxNzUxNzc1ODUwLCJleHAiOjE3NTE3NzU4NTF9.nVNqvIJZAUplcidwn32tJcndXKXs45Lu4NPyM6a_geIFK0_lyrEV73f_uqeCE4so1KcJ-X4JNGgHK4s2GKffzw';
        if (token) {
            config.headers['access-token'] = `${token}`;
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config;
    },
    function (error) {
        // 요청 오류가 있는 작업 수행
        return Promise.reject(error);
    }
);

axiosInstance.interceptors.response.use(
    (response) => response,
    (error) => {
        console.log('에러 인터셉터 진입 ✅');

        if (!error.response) {
            console.error('응답 없음 (CORS 등)', error);
            return Promise.reject(error);
        }

        const status = error.response.status;
        const code = error.response.data?.code;
        const message = error.response.data?.message;

        console.log('HTTP 상태 코드:', status);
        console.log('응답 코드:', code);
        console.log('message:', message);

        if (status === 401 && code === 'A002' && message === '토큰이 만료되었습니다.') {
            console.warn('🔐 토큰 만료로 인한 로그아웃 처리');
            removeCookies('accessToken');
            removeCookies('role');
            removeCookies('appUserId');
            removeCookies('userId');
            window.location.href = '/login';
        }

        return Promise.reject(error);
    }
);
