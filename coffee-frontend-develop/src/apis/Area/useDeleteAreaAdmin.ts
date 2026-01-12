import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { showToast } from '@/utils/showToast';

import { axiosInstance } from '../axiosInstance';

type DeleteAreaAdminReq = {
    areaId: number;
};

const deleteAreaAdmin = async ({ areaId }: DeleteAreaAdminReq) => {
    const response = await axiosInstance.delete(`/area/${areaId}`);
    return response.data;
};

export const useDeleteAreaAdmin = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteAreaAdmin,
        onSuccess: (res) => {
            if (res.code === 'SUCCESS') {
                queryClient.invalidateQueries({
                    queryKey: QUERY_KEYS.AREA.getAreaWithSectionList(),
                });
                showToast.success('삭제를 성공했습니다.');
                return;
            }
            showToast.error('삭제를 실패해습니다.');
            return;
        },
        onError: (error) => {
            const err = error as AxiosError<{ message?: string }>;
            showToast.error(`${err.response?.data.message ?? ''}`);
        },
    });
};
