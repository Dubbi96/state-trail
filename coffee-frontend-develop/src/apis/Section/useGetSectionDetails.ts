import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';
import { TSection } from '@/typings/Area';

type GetSectionDetailsRes = TSection;

type GetSectionDetailsReq = {
    sectionId: number | undefined;
};

const getSectionDetails = async (param: GetSectionDetailsReq): Promise<GetSectionDetailsRes> => {
    const { data } = await axiosInstance.get(`/section/${param.sectionId}`);

    return data.response;
};

export const useGetSectionDetails = (param: GetSectionDetailsReq) => {
    return useQuery<GetSectionDetailsRes>({
        queryKey: QUERY_KEYS.SECTION.getSectionDetails(JSON.stringify(param.sectionId)),
        queryFn: () => getSectionDetails(param),
        enabled: Boolean(param.sectionId),
    });
};
