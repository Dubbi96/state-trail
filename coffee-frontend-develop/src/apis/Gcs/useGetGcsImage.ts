import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';

type GetGcsImageReq = {
    fileUrl: string;
};
type GetGcsImageRes = {
    url: string;
};

const getGcsImage = async (param: GetGcsImageReq): Promise<GetGcsImageRes> => {
    return await axiosInstance.get(`/gcs/image?fileUrl=${param.fileUrl}`).then((res) => {
        if (res.data.code === 'SUCCESS') {
            return res.data.response;
        }
        throw new Error('GCS 이미지를 불러오는데 오류가 발생했습니다.');
    });
};

export const useGetGcsImage = (param: GetGcsImageReq) => {
    return useQuery({
        queryKey: QUERY_KEYS.GCS.getImagePreview(JSON.stringify(param)),
        queryFn: () => getGcsImage(param),
        enabled: Boolean(param),
    });
};
