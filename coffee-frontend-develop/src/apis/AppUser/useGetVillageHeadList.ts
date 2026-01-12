import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '../QueryKeys';
import { axiosInstance } from '../axiosInstance';

export type GetVillageHeadListRes = {
    areaInfo: {
        areaId: number;
        areaName: string;
    };
    sectionInfo: {
        sectionId: number;
        sectionName: string;
    };
    appUserId: string;
    appUserName: string;
    sectionName: string;
    farmerCount: number;
    id: number;
};
const getVillageHeadList = async (): Promise<GetVillageHeadListRes[]> => {
    return await axiosInstance.get('/app-user/village-heads').then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('면장 목록을 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetVillageHeadList = () => {
    return useQuery({
        queryKey: QUERY_KEYS.APP_USER.getVillageHeadList(),
        queryFn: () => getVillageHeadList(),
    });
};
