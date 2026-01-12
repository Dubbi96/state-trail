import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '../QueryKeys';
import { axiosInstance } from '../axiosInstance';

type GetVillageHeadDetailsReq = string | undefined;
type GetVillageHeadDetailsRes = {
    userId: string;
    username: string;
    accountInfo: string;
    bankName: string;
    identificationPhotoUrl: string;
    contractFileUrl: string;
    bankbookPhotoUrl: string;
    areaInfo: { areaId : number;longitude: number; latitude: number; areaName: string };
    sectionInfo: {
        longitude: number;
        latitude: number;
        sectionName: string;
        sectionId: number;
    };
};

const getVillageHeadDetails = async (
    param: GetVillageHeadDetailsReq
): Promise<GetVillageHeadDetailsRes> => {
    return await axiosInstance.get(`/app-user/village-head/${param}`).then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('면장 정보를 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetVillageHeadDetails = (param: GetVillageHeadDetailsReq) => {
    return useQuery({
        queryKey: QUERY_KEYS.APP_USER.getVillageHeadDetail(JSON.stringify(param)),
        queryFn: () => getVillageHeadDetails(param),
        enabled: Boolean(param),
    });
};
