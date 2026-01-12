import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';
import { TArea } from '@/typings/Area';

type GetAreaDetailsRes = TArea;

type GetAreaDetailsReq = {
    areaId: number | undefined;
};

const getAreaDetails = async (param: GetAreaDetailsReq): Promise<GetAreaDetailsRes> => {
    const { data } = await axiosInstance.get(`/area/${param.areaId}`);

    return data.response;
};

export const useGetAreaDetails = (param: GetAreaDetailsReq) => {
    return useQuery<GetAreaDetailsRes>({
        queryKey: QUERY_KEYS.AREA.getAreaDetails(JSON.stringify(param.areaId)),
        queryFn: () => getAreaDetails(param),
        enabled: Boolean(param.areaId),
    });
};
