import { useMutation } from '@tanstack/react-query';

import { axiosInstance } from '@/apis/axiosInstance';
import { TGcsDirectory } from '@/typings/Gcs';

export type PostGcsImageReq = {
    directory: TGcsDirectory;
    file: File;
};

const postGcsImage = async (param: PostGcsImageReq) => {
    return await axiosInstance.post('/gcs/image', param);
};

export const usePostGcsImage = () => {
    return useMutation({
        mutationFn: (param: PostGcsImageReq) => postGcsImage(param),
    });
};
