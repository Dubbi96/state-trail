import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';
import { TSection } from '@/typings/Area';

type GetAreaSectionListRes = {
    id: number;
    areaName: string;
    longitude: number;
    latitude: number;
    sections: TSection[];
};

const getSectionList = async (areaId: number | undefined): Promise<GetAreaSectionListRes[]> => {
    return await axiosInstance.get(`/area/${areaId}/with-sections`).then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('지역 및 섹션 리스트를 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetSectionList = (areaId: number | undefined) => {
    return useQuery({
        queryKey: QUERY_KEYS.AREA.getSectionList(JSON.stringify(areaId) || ''),
        queryFn: () => getSectionList(areaId),
        enabled: !!areaId,
    });
};
