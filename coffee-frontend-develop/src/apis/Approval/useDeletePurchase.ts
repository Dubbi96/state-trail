import { useMutation, useQueryClient } from '@tanstack/react-query';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { showToast } from '@/utils/showToast';

import { axiosInstance } from '../axiosInstance';

type DeletePurchaseReq = {
    purchaseId: number;
};

const deletePurchase = async ({ purchaseId }: DeletePurchaseReq) => {
    const response = await axiosInstance.delete(`/approval/purchase/${purchaseId}`, {
        params: { approverId: 1 },
    });
    return response.data;
};

export const useDeletePurchase = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: deletePurchase,
        onSuccess: (res) => {
            if (res.code === 'SUCCESS') {
                queryClient.invalidateQueries({
                    queryKey: QUERY_KEYS.PURCHASE.all(),
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
