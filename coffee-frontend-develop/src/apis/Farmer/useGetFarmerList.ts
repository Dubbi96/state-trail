import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';

export type GetFarmerListRes = {
    id: number;
    farmerName: string;
    villageHeadName: string;
    sectionName: string;
};
const getFarmerList = async (): Promise<GetFarmerListRes[]> => {
    return await axiosInstance.get('/farmer').then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('농부 목록을 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetFarmerList = () => {
    return useQuery({
        queryKey: QUERY_KEYS.FARMER.getFarmerList(),
        queryFn: () => getFarmerList(),
    });
};
