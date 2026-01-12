import { useQuery } from '@tanstack/react-query';
import { Dayjs } from 'dayjs';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';
import { TPurchase } from '@/typings/Purchase';

export type GetPurchaseListReq = {
    startDate?: Dayjs;
    endDate?: Dayjs;
    villageHeadId?: string;
};

export type GetPurchaseList = TPurchase;

const getPurchaseList = async (param: GetPurchaseListReq): Promise<GetPurchaseList[]> => {
    const startDate = param.startDate ? `?startDate=${param.startDate.format('YYYY-MM-DD')}` : '';
    const endDate = param.endDate ? `&endDate=${param.endDate.format('YYYY-MM-DD')}` : '';
    const villageHeadId = param.villageHeadId ? `&villageHeadId=${param.villageHeadId}` : '';

    return await axiosInstance
        .get(`/purchase${startDate}${endDate}${villageHeadId}`)
        .then((res) => {
            if (res.data.code === 'SUCCESS') {
                return res.data.response;
            }
        });
};

export const useGetPurchaseList = (param: GetPurchaseListReq) => {
    return useQuery({
        queryKey: QUERY_KEYS.PURCHASE.getPurchaseList(JSON.stringify(param)),
        queryFn: () => getPurchaseList(param),
    });
};
