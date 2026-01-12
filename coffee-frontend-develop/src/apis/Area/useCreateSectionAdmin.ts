import { useMutation } from '@tanstack/react-query';

import { CreateSectionReq } from '@/apis/Area/useCreateSection';
import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance, queryClient } from '@/apis/axiosInstance';

const createSectionAdmin = async (param: CreateSectionReq) => {
    return await axiosInstance.post('/section', param);
};

export const useCreateSectionAdmin = () => {
    return useMutation({
        mutationFn: (param: CreateSectionReq) => createSectionAdmin(param),
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.AREA.all(),
            });
        },
    });
};
