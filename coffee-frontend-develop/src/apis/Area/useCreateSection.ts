import { useMutation } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance, queryClient } from '@/apis/axiosInstance';

export type CreateSectionReq = {
    sectionName: string;
    latitude: number;
    longitude: number;
    areaId: number;
};

const createSection = async (param: CreateSectionReq) => {
    return await axiosInstance.post('/approval/section?approverId=1', param);
};

export const useCreateSection = () => {
    return useMutation({
        mutationFn: (param: CreateSectionReq) => createSection(param),
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.AREA.all(),
            });
        },
    });
};
