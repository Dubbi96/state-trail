import { useMutation, useQueryClient } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { showToast } from '@/utils/showToast';

import { axiosInstance } from '../axiosInstance';

type DeleteFarmerReq = {
    farmerId: number;
};

const deleteFarmer = async ({ farmerId }: DeleteFarmerReq) => {
    const response = await axiosInstance.delete(`/approval/farmer/${farmerId}`, {
        params: { approverId: 1 },
    });
    return response.data;
};

export const useDeleteFarmer = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deleteFarmer,
        onSuccess: (res) => {
            if (res.code === 'SUCCESS') {
                queryClient.invalidateQueries({
                    queryKey: QUERY_KEYS.FARMER.getFarmerList(),
                });
                showToast.success('삭제 요청을 성공했습니다.');
                return;
            }
            showToast.error('삭제 요청을 실패해습니다.');
            return;
        },
        onError: (res) => {
            showToast.error('삭제 요청을 실패해습니다.' + `\n${res.message ?? ''}`);
        },
    });
};
