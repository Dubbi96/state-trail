import { useMutation } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { axiosInstance } from '@/apis/axiosInstance';

export type UpdateApprovalTreePurchaseReq = {
    id: number;
    villageHeadId: string;
    deduction: number;
    paymentAmount: number;
    purchaseDate: string;
    quantity: number;
    totalPrice: number;
    unitPrice: number;
    remarks: string;
};

const updateApprovalTreePurchase = async (
    param: UpdateApprovalTreePurchaseReq
): Promise<AxiosResponse> => {
    const requestBody = {
        id: Number(param.id),
        villageHeadId: Number(param.villageHeadId),
        deduction: Number(param.deduction),
        paymentAmount: Number(param.paymentAmount),
        purchaseDate: param.purchaseDate,
        quantity: Number(param.quantity),
        totalPrice: Number(param.totalPrice),
        unitPrice: Number(param.unitPrice),
        remarks: param.remarks,
    };

    return await axiosInstance.patch(`/approval/purchase/${param.id}?approverId=1`, requestBody);
};

export const useUpdateApprovalTreePurchase = () => {
    return useMutation({
        mutationFn: (param: UpdateApprovalTreePurchaseReq) => updateApprovalTreePurchase(param),
        onSuccess: () => {},
    });
};
