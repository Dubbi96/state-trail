import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { QUERY_KEYS } from '@/apis/QueryKeys';
import { axiosInstance } from '@/apis/axiosInstance';

export type CreateApprovalPurchaseReq = {
    villageHeadId: number;
    deduction: number;
    paymentAmount: number;
    purchaseDate: string;
    quantity: number;
    totalPrice: number;
    unitPrice: number;
    remarks?: string;
};

const createApprovalPurchase = async (param: CreateApprovalPurchaseReq): Promise<AxiosResponse> => {
    return await axiosInstance.post(`/approval/purchase?approverId=1`, param);
};

export const useCreateApprovalPurchase = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (param: CreateApprovalPurchaseReq) => createApprovalPurchase(param),
        onSuccess: () =>
            queryClient.invalidateQueries({
                queryKey: QUERY_KEYS.PURCHASE.all(),
            }),
    });
};
