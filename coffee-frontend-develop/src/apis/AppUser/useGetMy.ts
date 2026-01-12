import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';
import { TRole } from '@/typings/User';

import { getCookies } from './cookie';

type GetMyRes = {
    appUser: {
        id: number;
        userId: string;
        username: string;
        role: TRole;
    };
    area?: {
        areaName: string;
        id: number;
        latitude: number;
        longitude: number;
    };
    idCardUrl?: string | null;
};

const getMy = async (): Promise<GetMyRes> => {
    return await axiosInstance.get(`/app-user/my`).then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('내 정보를 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetMy = () => {
    const appUserId = getCookies('appUserId');
    return useQuery({
        queryKey: QUERY_KEYS.APP_USER.my(appUserId),
        queryFn: () => getMy(),
        staleTime: 0, // 데이터를 항상 오래되지 않은 것으로 간주하지 않음
        refetchOnMount: true, // 마운트마다 refetch
        refetchOnWindowFocus: false,
    });
};
