import { useMutation } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { axiosInstance } from '@/apis/axiosInstance';

export type CreateApprovalFarmerRegisterReq = {
    name: string;
    villageHeadId: number | null;
    approverId: number | null;
    identificationPhoto?: File | null;
};
const createApprovalFarmerRegister = async (
    param: CreateApprovalFarmerRegisterReq
): Promise<AxiosResponse> => {
    const query = new URLSearchParams({
        name: param.name,
        villageHeadId: String(param.villageHeadId),
        approverId: '1',
    }).toString();
    const formData = new FormData();

    if (param.identificationPhoto) {
        formData.append('identificationPhoto', param.identificationPhoto);
    }
    return await axiosInstance.post(`/approval/farmer?${query}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const useCreateApprovalFarmerRegister = () => {
    return useMutation({
        mutationFn: (param: CreateApprovalFarmerRegisterReq) => createApprovalFarmerRegister(param),
    });
};
