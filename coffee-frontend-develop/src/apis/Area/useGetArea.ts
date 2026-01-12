import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { TArea } from '@/typings/Area';

import { axiosInstance } from '../axiosInstance';

type GetAreaRes = TArea[];

const getArea = async (): Promise<GetAreaRes> => {
    const { data } = await axiosInstance.get(`/area`);

    return data.response;
};

export const useGetArea = () => {
    return useQuery<GetAreaRes>({
        queryKey: QUERY_KEYS.AREA.getAreaList(),
        queryFn: getArea,
    });
};
