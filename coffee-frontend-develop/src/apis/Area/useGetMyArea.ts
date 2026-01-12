import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { TArea } from '@/typings/Area';

import { axiosInstance } from '../axiosInstance';

type GetMyAreaRes = TArea;

const getMyArea = async (): Promise<GetMyAreaRes> => {
    const { data } = await axiosInstance.get(`/area/my`);

    return data.response;
};

export const useGetMyArea = () => {
    return useQuery<GetMyAreaRes>({
        queryKey: QUERY_KEYS.AREA.getMyArea(),
        queryFn: getMyArea,
    });
};

///area/my
