import { useMutation } from '@tanstack/react-query';
import { AxiosResponse } from 'axios';

import { axiosInstance } from '@/apis/axiosInstance';

export type CreateApprovalVillageHeadRegisterReq = {
    userId: string;
    username: string;
    password: string;
    bankName?: string;
    accountInfo?: string;
    sectionId: number;
    identificationPhoto: File;
    contractFile: File;
    bankbookPhoto: File;
    approverId: string;
    photo?: string; //[TODO] 아직 받아오고 있지 않음.
};

const createApprovalVillageHeadRegister = async (
    param: CreateApprovalVillageHeadRegisterReq
): Promise<AxiosResponse> => {
    const queryParams: Record<string, string> = {
        userId: param.userId,
        username: param.username,
        password: param.password,
        sectionId: String(param.sectionId),
        approverId: '1',
    };

    if (param.bankName) queryParams.bankName = param.bankName;
    if (param.accountInfo) queryParams.accountInfo = param.accountInfo;

    const query = new URLSearchParams(queryParams).toString();

    const formData = new FormData();
    formData.append('identificationPhoto', param.identificationPhoto);
    formData.append('contractFile', param.contractFile);
    formData.append('bankbookPhoto', param.bankbookPhoto);

    return await axiosInstance.post(`/approval/village-head?${query}`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
};

export const useCreateApprovalVillageHeadRegister = () => {
    return useMutation({
        mutationFn: (param: CreateApprovalVillageHeadRegisterReq) =>
            createApprovalVillageHeadRegister(param),
    });
};
