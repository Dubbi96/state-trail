import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';
import { TAreaWithSections } from '@/typings/Area';

type GetAreaWithSectionListRes = TAreaWithSections[];

const getAreaWithSectionList = async (): Promise<GetAreaWithSectionListRes> => {
    return await axiosInstance.get('/area/with-sections').then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('지역 및 섹션 리스트를 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetAreaWithSectionList = () => {
    return useQuery({
        queryKey: QUERY_KEYS.AREA.getAreaWithSectionList(),
        queryFn: () => getAreaWithSectionList(),
    });
};
