import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';

export type GetViceAdminListRes = {
    id: number;
    userName: string;
    userId: string;
    areaInfo: {
        longitude: number;
        latitude: number;
        areaName: string;
    };
};

const getViceAdminList = async (): Promise<GetViceAdminListRes[]> => {
    return await axiosInstance.get('/app-user/vice-admins').then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('부관리자 목록을 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetViceAdminList = () => {
    return useQuery({
        queryKey: QUERY_KEYS.APP_USER.getViceAdminList(),
        queryFn: () => getViceAdminList(),
    });
};
