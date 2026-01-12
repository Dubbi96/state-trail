import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';

type GetViceAdminDetailsReq = string | undefined;
type GetViceAdminDetailsRes = {
    id: number;
    userId: string;
    username: string;
    idCardUrl: string;
    areaInfo: {
        areaId: number;
        longitude: number;
        latitude: number;
        areaName: string;
    };
};

const getViceAdminDetails = async (
    param: GetViceAdminDetailsReq
): Promise<GetViceAdminDetailsRes> => {
    return await axiosInstance.get(`/app-user/vice-admin/${param}`).then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('부관리자 정보를 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetViceAdminDetails = (param: GetViceAdminDetailsReq) => {
    return useQuery({
        queryKey: QUERY_KEYS.APP_USER.getViceAdminDetail(JSON.stringify(param)),
        queryFn: () => getViceAdminDetails(param),
        enabled: Boolean(param),
    });
};
